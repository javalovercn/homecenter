package hc.server.rms;

import hc.core.cache.RecordWriter;
import hc.core.util.ByteUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 线程安全。
 *
 */
public class J2SERMSRecordWriter implements RecordWriter {
	private static final boolean isDebug = false;

	public static final int USER_BLOCK_SIZE = 1024;

	// ---------------------------------------------------------------------------------------------------------------------
	// 每个数据块结构如下（非首记录表）
	// ---------------------------------------------------------------------------------------------------------------------
	// [using_size 4位] [next_block_no 4位] [user_data]
	public static final int IDX_LEN4_USER_USING_SIZE = 0;// 当前块使用的字节数，4位
	public static final int IDX_LEN4_USER_NEXT_BLOCK_NO = IDX_LEN4_USER_USING_SIZE + 4;// 下一块物理索引，4位。如果没有下一个块，则置为0
	public static final int IDX_USER_DATA = IDX_LEN4_USER_NEXT_BLOCK_NO + 4;// 数据写入起始位

	public static final int MAX_USER_BLOCK_DATA_NUM = USER_BLOCK_SIZE - IDX_USER_DATA;

	final byte[] standardBlockBuf = new byte[USER_BLOCK_SIZE];
	int headerBufEndIdx = USER_BLOCK_SIZE;
	byte[] headerBuf = new byte[headerBufEndIdx];
	final byte[] fourByteBuf = new byte[4];

	final RandomAccessFile raf;

	long emptyStartBlockNo, emptyEndBlockNo, lastFileBlockNo;
	// ---------------------------------------------------------------------------------------------------------------------
	// 首记录结构如下，注意：起始记录号为1，不是0，这是RMS的规范
	// ---------------------------------------------------------------------------------------------------------------------
	// ------[last_tail] [empty_first] [empty_end]-------[rec_1] [start_no]
	// [end_no]...-------[0, if del, never use again] [start_no] [end_no]
	private static final int IDX_LEN4_HEADER_LAST_FILE_BLOCK = 0;
	private static final int IDX_LEN4_HEADER_EMPTY_START_BLOCK = IDX_LEN4_HEADER_LAST_FILE_BLOCK + 4;
	private static final int IDX_LEN4_HEADER_EMPTY_END_BLOCK = IDX_LEN4_HEADER_EMPTY_START_BLOCK + 4;
	private static final int IDX_LEN4_HEADER_LAST_FILE_BLOCK_ABS = IDX_USER_DATA;
	private static final int IDX_LEN4_HEADER_EMPTY_START_BLOCK_ABS = IDX_LEN4_HEADER_EMPTY_START_BLOCK + IDX_USER_DATA;
	private static final int IDX_LEN4_HEADER_EMPTY_END_BLOCK_ABS = IDX_LEN4_HEADER_EMPTY_END_BLOCK + IDX_USER_DATA;
	private static final int IDX_HEADER_FIRST_USER_BLOCK_IDX = IDX_LEN4_HEADER_EMPTY_END_BLOCK + 4;
	private static final int HEADER_USER_BLOCK_LEN = 4 + 4 * 2;// 4，逻辑块索引编号；4，块存储首物理块索引；4块存储尾物理块索引

	static final ByteArrayOutputStream baos = new ByteArrayOutputStream(USER_BLOCK_SIZE * 200);

	final String logicTableName;

	@Override
	public String getLogicTableName() {
		return logicTableName;
	}

	public J2SERMSRecordWriter(final RandomAccessFile raf, final String logicTableName) throws Exception {
		this.logicTableName = logicTableName;
		this.raf = raf;

		if (raf.length() == 0) {
			// 初始化第一个块
			raf.seek(0);
			raf.write(headerBuf);

			lastFileBlockNo = 1;

			headerBufEndIdx = HEADER_USER_BLOCK_LEN;

			// 仅使用头块，初始设置下一个可用块索引
			ByteUtil.integerToFourBytes((int) lastFileBlockNo, headerBuf, IDX_LEN4_HEADER_LAST_FILE_BLOCK);

			raf.seek(IDX_LEN4_HEADER_LAST_FILE_BLOCK_ABS);
			raf.write(headerBuf, 0, headerBufEndIdx);

			// 初始块字节使用量
			raf.seek(0);
			ByteUtil.integerToFourBytes(headerBufEndIdx, fourByteBuf, 0);
			raf.write(fourByteBuf, 0, 4);

			// //初始块的下一个块索引为0
			// raf.seek(2);
			// ByteUtil.integerToFourBytes(0, fourByteBuf, 0);
			// raf.write(fourByteBuf, 0, 4);
		} else {
			final byte[] firstLinkHeader = new byte[IDX_USER_DATA];
			raf.seek(0);
			raf.read(firstLinkHeader);

			final byte[] storedHeaderBuf = readStream(0);
			if (storedHeaderBuf.length > headerBuf.length) {
				headerBuf = baos.toByteArray();
				headerBufEndIdx = headerBuf.length;
			} else {
				System.arraycopy(storedHeaderBuf, 0, headerBuf, 0, storedHeaderBuf.length);
				headerBufEndIdx = storedHeaderBuf.length;
			}

			lastFileBlockNo = ByteUtil.fourBytesToLong(headerBuf, IDX_LEN4_HEADER_LAST_FILE_BLOCK);
			emptyStartBlockNo = ByteUtil.fourBytesToLong(headerBuf, IDX_LEN4_HEADER_EMPTY_START_BLOCK);
			emptyEndBlockNo = ByteUtil.fourBytesToLong(headerBuf, IDX_LEN4_HEADER_EMPTY_END_BLOCK);
		}

	}

	private final byte[] readStream(final long startBlockNo) throws Exception {
		synchronized (baos) {// 注意：由于是表态变量，全局共享，所以加锁。
			baos.reset();

			long nextBlockNo = startBlockNo;

			do {
				raf.seek(nextBlockNo * USER_BLOCK_SIZE);
				raf.read(standardBlockBuf);

				if (isDebug) {
					System.out.println("read block [" + nextBlockNo + "]");
				}

				final long currLen = ByteUtil.fourBytesToLong(standardBlockBuf, IDX_LEN4_USER_USING_SIZE);
				baos.write(standardBlockBuf, IDX_USER_DATA, (int) currLen);

				nextBlockNo = ByteUtil.fourBytesToLong(standardBlockBuf, IDX_LEN4_USER_NEXT_BLOCK_NO);

			} while (nextBlockNo > 0);

			return baos.toByteArray();
		}
	}

	/**
	 * 将
	 * 
	 * @param startBlockNo
	 */
	private final void addUnusedOverrideTailToEmpty(long startBlockNo, final long endBlockNo) throws Exception {
		if (startBlockNo == 0) {// 一个Overrid段，正好全使用完，到0出现
			return;
		}

		if (emptyStartBlockNo == 0) {
			emptyStartBlockNo = startBlockNo;
			emptyEndBlockNo = endBlockNo;

			if (isDebug) {
				System.out.println("free blocks start-end [" + emptyStartBlockNo + "-" + emptyEndBlockNo + "]");
			}

			ByteUtil.integerToFourBytes((int) emptyStartBlockNo, headerBuf, IDX_LEN4_HEADER_EMPTY_START_BLOCK);
		} else {
			long preStartBlockNo;
			while (true) {
				final long pos = startBlockNo * USER_BLOCK_SIZE;
				raf.seek(pos + IDX_LEN4_USER_NEXT_BLOCK_NO);
				raf.read(fourByteBuf);

				preStartBlockNo = ByteUtil.fourBytesToLong(fourByteBuf, 0);// 读取出后继块号

				if (isDebug) {
					System.out.println("free block [" + startBlockNo + "]");
				}

				// 新添加的首块，追加到旧empty尾部，
				raf.seek(emptyEndBlockNo * USER_BLOCK_SIZE + IDX_LEN4_USER_NEXT_BLOCK_NO);
				ByteUtil.integerToFourBytes((int) startBlockNo, fourByteBuf, 0);// 更新下一条的记录号
				raf.write(fourByteBuf, 0, 4);

				emptyEndBlockNo = startBlockNo;

				if (preStartBlockNo == 0) {
					break;
				} else {
					startBlockNo = preStartBlockNo;
				}
			}
		}

		ByteUtil.integerToFourBytes((int) emptyEndBlockNo, headerBuf, IDX_LEN4_HEADER_EMPTY_END_BLOCK);

		raf.seek(IDX_LEN4_HEADER_EMPTY_START_BLOCK_ABS);
		raf.write(headerBuf, IDX_LEN4_HEADER_EMPTY_START_BLOCK, 4 * 2);
	}

	/**
	 * 
	 * @param startBlockNo
	 * @param endBlockNo
	 * @param bais
	 * @return 返回最后已更新的块号，以此更新到header的索引块中
	 * @throws Exception
	 */
	public final long writeStreamOverride(final long startBlockNo, final long endBlockNo, final ByteArrayInputStream bais)
			throws Exception {
		long nextBlockNo = startBlockNo;
		long preNextBlockNo;
		do {
			final long pos = nextBlockNo * USER_BLOCK_SIZE;
			logUse(nextBlockNo);
			raf.seek(pos + IDX_LEN4_USER_NEXT_BLOCK_NO);
			raf.read(fourByteBuf);

			preNextBlockNo = ByteUtil.fourBytesToLong(fourByteBuf, 0);

			final int currWriteLen = bais.read(standardBlockBuf, IDX_USER_DATA, MAX_USER_BLOCK_DATA_NUM);// 装入数据写入块

			if (currWriteLen <= MAX_USER_BLOCK_DATA_NUM) {
				boolean hasNextBlock = false;
				if (currWriteLen == MAX_USER_BLOCK_DATA_NUM) {
					if (bais.available() == 0) {
					} else {
						hasNextBlock = true;
					}
				}

				{
					final long nextStoreRecordNo;

					if (hasNextBlock == false) {
						addUnusedOverrideTailToEmpty(preNextBlockNo, endBlockNo);
						nextStoreRecordNo = 0;
					} else if (nextBlockNo == endBlockNo) {
						nextStoreRecordNo = (int) getNextEmptyBlockNo();
					} else {
						nextStoreRecordNo = preNextBlockNo;
					}
					ByteUtil.integerToFourBytes((int) nextStoreRecordNo, standardBlockBuf, IDX_LEN4_USER_NEXT_BLOCK_NO);
				}

				ByteUtil.integerToFourBytes(currWriteLen, standardBlockBuf, IDX_LEN4_USER_USING_SIZE);
				raf.seek(pos);
				raf.write(standardBlockBuf);

				if (hasNextBlock == false) {
					return nextBlockNo;
				} else if (nextBlockNo == endBlockNo) {
					return writeStreamToEmptyLink(bais);
				}

				nextBlockNo = preNextBlockNo;
			}
		} while (true);
	}

	private final long getNextEmptyBlockNo() {
		// 检查空位，返回空位，如果没有，返回IDX_4_HEADER_LAST_FILE_BLOCK
		if (emptyStartBlockNo == 0) {
			return lastFileBlockNo;
		} else {
			return emptyStartBlockNo;
		}
	}

	/**
	 * @param bais
	 * @return 返回最后已更新的块号，以此更新到header的索引块中
	 * @throws Exception
	 */
	public final long writeStreamToEmptyLink(final ByteArrayInputStream bais) throws Exception {
		if (emptyStartBlockNo == 0) {
			return writeStreamTailAppend(bais);
		}

		long nextBlockNo = emptyStartBlockNo;
		long preNextBlockNo;
		boolean isNoEmptyLink = false;
		do {
			final long pos = nextBlockNo * USER_BLOCK_SIZE;

			logUse(nextBlockNo);

			raf.seek(pos + IDX_LEN4_USER_NEXT_BLOCK_NO);
			raf.read(fourByteBuf);

			preNextBlockNo = ByteUtil.fourBytesToLong(fourByteBuf, 0);
			if (preNextBlockNo == 0) {
				preNextBlockNo = lastFileBlockNo;
				isNoEmptyLink = true;
			}

			final int currWriteLen = bais.read(standardBlockBuf, IDX_USER_DATA, MAX_USER_BLOCK_DATA_NUM);

			if (currWriteLen <= MAX_USER_BLOCK_DATA_NUM) {
				boolean hasNext = false;
				if (currWriteLen == MAX_USER_BLOCK_DATA_NUM && bais.available() > 0) {
					hasNext = true;
				}

				ByteUtil.integerToFourBytes((int) preNextBlockNo, standardBlockBuf, IDX_LEN4_USER_NEXT_BLOCK_NO);
				ByteUtil.integerToFourBytes(currWriteLen, standardBlockBuf, IDX_LEN4_USER_USING_SIZE);

				raf.seek(nextBlockNo * USER_BLOCK_SIZE);
				raf.write(standardBlockBuf);

				if (hasNext == false || isNoEmptyLink) {
					if (hasNext == false) {
						if (isNoEmptyLink) {
							emptyStartBlockNo = 0;
							emptyEndBlockNo = 0;
						} else {
							emptyStartBlockNo = preNextBlockNo;
						}
					} else {
						emptyStartBlockNo = 0;
						emptyEndBlockNo = 0;
					}

					updateBlockNoToHeaderAndStorage(IDX_LEN4_HEADER_EMPTY_START_BLOCK, emptyStartBlockNo, true);// 必须保存
					updateBlockNoToHeaderAndStorage(IDX_LEN4_HEADER_EMPTY_END_BLOCK, emptyEndBlockNo, true);

					if (hasNext == false) {
						return nextBlockNo;
					} else {
						return writeStreamTailAppend(bais);
					}
				}

				nextBlockNo = preNextBlockNo;
			}
		} while (true);
	}

	/**
	 * @param bais
	 * @return 返回最后已更新的块号，以此更新到header的索引块中
	 * @throws Exception
	 */
	public final long writeStreamTailAppend(final ByteArrayInputStream bais) throws Exception {
		do {
			final int currWriteLen = bais.read(standardBlockBuf, IDX_USER_DATA, MAX_USER_BLOCK_DATA_NUM);

			if (currWriteLen <= MAX_USER_BLOCK_DATA_NUM) {
				boolean hasNext = false;
				if (currWriteLen == MAX_USER_BLOCK_DATA_NUM && bais.available() > 0) {
					hasNext = true;
				}

				if (hasNext) {
					ByteUtil.integerToFourBytes((int) (lastFileBlockNo + 1), standardBlockBuf, IDX_LEN4_USER_NEXT_BLOCK_NO);
				} else {
					ByteUtil.integerToFourBytes(0, standardBlockBuf, IDX_LEN4_USER_NEXT_BLOCK_NO);// 收尾，标记后继块号为0
				}
				logUse(lastFileBlockNo);
				ByteUtil.integerToFourBytes(currWriteLen, standardBlockBuf, IDX_LEN4_USER_USING_SIZE);
				raf.seek(lastFileBlockNo * USER_BLOCK_SIZE);
				raf.write(standardBlockBuf);

				lastFileBlockNo++;

				if (hasNext == false) {
					ByteUtil.integerToFourBytes((int) lastFileBlockNo, headerBuf, IDX_LEN4_HEADER_LAST_FILE_BLOCK);
					raf.seek(IDX_LEN4_HEADER_LAST_FILE_BLOCK_ABS);
					raf.write(headerBuf, IDX_LEN4_HEADER_LAST_FILE_BLOCK, 4);

					return lastFileBlockNo - 1;
				}
			}
		} while (true);
	}

	/**
	 * 返回找到的idx，如果没有，则返回-1
	 * 
	 * @param recordID
	 * @return
	 */
	private final int searchRecordIDFromHeader(final int recordID) {
		final int searchEndIdx = headerBufEndIdx;

		for (int i = HEADER_USER_BLOCK_LEN; i < searchEndIdx;) {// 忽略[last_tail]
																// [empty_first]
																// [empty_end]
			final long currRecordID = ByteUtil.fourBytesToLong(headerBuf, i);

			if (currRecordID == recordID) {
				return i;
			}
			i += HEADER_USER_BLOCK_LEN;
		}

		return -1;
	}

	@Override
	public final synchronized void setRecord(final int recordId, final byte[] data, final int offset, final int len) throws Exception {
		final int recordBlockIdx = searchRecordIDFromHeader(recordId);
		if (recordBlockIdx == -1) {
			throw new Exception("InvalidRecordIDException");
		}

		final ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, len);
		final long startBlockNo = ByteUtil.fourBytesToLong(headerBuf, recordBlockIdx + 4);
		final int endBlockHeaderIdx = recordBlockIdx + 8;
		final long endBlockNo = ByteUtil.fourBytesToLong(headerBuf, endBlockHeaderIdx);

		if (isDebug) {
			System.out.println("\n>>>override writing data...");
		}
		final long writeAfterEndBlock = writeStreamOverride(startBlockNo, endBlockNo, bais);
		if (isDebug) {
			System.out.println("<<<end override writing data.\n");
		}

		updateEndBlockNoNextBlockToZero(writeAfterEndBlock);
		updateBlockNoToHeaderAndStorage(endBlockHeaderIdx, writeAfterEndBlock, true);
	}

	private final void updateEndBlockNoNextBlockToZero(final long endBlockNo) throws Exception {
		if (endBlockNo == 0) {
			return;
		}

		final long pos = endBlockNo * USER_BLOCK_SIZE;

		raf.seek(pos + IDX_LEN4_USER_NEXT_BLOCK_NO);

		ByteUtil.integerToFourBytes(0, fourByteBuf, 0);
		raf.write(fourByteBuf, 0, fourByteBuf.length);
	}

	private final void updateBlockNoToHeaderAndStorage(final int headerIdx, final long BlockNo, final boolean withStorage)
			throws IOException {
		ByteUtil.integerToFourBytes((int) BlockNo, headerBuf, headerIdx);

		if (withStorage) {
			raf.seek(IDX_USER_DATA + headerIdx);
			raf.write(headerBuf, headerIdx, 4);
		}
	}

	private final long searchNextRecordNo() {
		return (headerBufEndIdx - HEADER_USER_BLOCK_LEN) / HEADER_USER_BLOCK_LEN + 1;// 删除的记录号仍占位，所以表的后继号可直接计算出来。
	}

	@Override
	public final synchronized int addRecord(final byte[] data, final int offset, final int len) throws Exception {
		final ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, len);

		// 写数据
		if (isDebug) {
			System.out.println("\n>>>writing data...");
		}
		final long startBlockNo, endBlockNo;
		if (emptyStartBlockNo > 0) {
			startBlockNo = emptyStartBlockNo;
			endBlockNo = writeStreamToEmptyLink(bais);
		} else {
			startBlockNo = lastFileBlockNo;
			endBlockNo = writeStreamTailAppend(bais);
		}
		if (isDebug) {
			System.out.println("<<<end writing data.\n");
		}

		updateEndBlockNoNextBlockToZero(endBlockNo);

		final long nextRecordNo = searchNextRecordNo();

		final int nextHeaderEndIdx = headerBufEndIdx + HEADER_USER_BLOCK_LEN;
		if (nextHeaderEndIdx > headerBuf.length) {
			final byte[] newHeader = new byte[headerBuf.length * 2];
			System.arraycopy(headerBuf, 0, newHeader, 0, headerBufEndIdx);
			headerBuf = newHeader;
		}

		updateBlockNoToHeaderAndStorage(headerBufEndIdx, nextRecordNo, false);
		updateBlockNoToHeaderAndStorage(headerBufEndIdx + 4, startBlockNo, false);
		updateBlockNoToHeaderAndStorage(headerBufEndIdx + 8, endBlockNo, false);

		headerBufEndIdx = nextHeaderEndIdx;

		raf.seek(IDX_LEN4_USER_NEXT_BLOCK_NO);
		raf.read(fourByteBuf);

		writeStreamOverride(0, ByteUtil.fourBytesToLong(fourByteBuf), new ByteArrayInputStream(headerBuf, 0, headerBufEndIdx));

		return (int) nextRecordNo;
	}

	private final void logUse(final long startBlockNo) {
		if (isDebug) {
			System.out.println("use block [" + startBlockNo + "]");
		}
	}

	@Override
	public final void closeRecordStore() throws Exception {
		raf.close();
	}

	@Override
	public final synchronized byte[] getRecord(final int recordId) throws Exception {
		final int recordBlockIdx = searchRecordIDFromHeader(recordId);
		if (recordBlockIdx == -1) {
			throw new Exception("InvalidRecordIDException");
		}

		final long startBlockNo = ByteUtil.fourBytesToLong(headerBuf, recordBlockIdx + 4);

		return readStream(startBlockNo);
	}

	@Override
	public final synchronized void deleteRecord(final int recordId) throws Exception {
		// 将header块中的索引号标记为0
		final int recordBlockIdx = searchRecordIDFromHeader(recordId);
		if (recordBlockIdx == -1) {
			throw new Exception("InvalidRecordIDException");
		}

		// 将不用的记录号，标记为0，以占位。生成下一记录号时，自动累进。
		ByteUtil.integerToFourBytes(0, headerBuf, recordBlockIdx);
		raf.seek(IDX_USER_DATA + recordBlockIdx);
		raf.write(headerBuf, recordBlockIdx, 4);

		// 不能先添加空间到EmptyLink，因为故障可能导致先入EmtpyLink后，断电，同时未置BlockNo为0，导致重复使用该段
		final long startBlockNo = ByteUtil.fourBytesToLong(headerBuf, recordBlockIdx + 4);
		final long endBlockNo = ByteUtil.fourBytesToLong(headerBuf, recordBlockIdx + 8);

		addUnusedOverrideTailToEmpty(startBlockNo, endBlockNo);
	}
}
