package hc.server.ui.design.code;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPriorityManager;
import hc.server.DefaultManager;
import hc.util.ClassUtil;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

public class DocHelper {
	private static final int cssSize = 120;
	private final HashMap<String, HashMap<String, String>> cache = new HashMap<String, HashMap<String, String>>();
	final ArrayList<CodeItem> cssCodeItems = new ArrayList<CodeItem>(cssSize);
	final HashMap<String, String> cssDocs = new HashMap<String, String>(cssSize);
	final HashMap<String, String> cssProperties = new HashMap<String, String>(DocHelper.cssSize);
	static final Pattern douhaoSpaces = Pattern.compile(", {2,}");

	final void buildPropCodeItems(final HashMap<String, String> props) {
		final Object[] keys = props.keySet().toArray();
		Arrays.sort(keys);

		final int size = keys.length;
		for (int j = 0; j < size; j++) {
			final CodeItem item = CodeItem.getFree();
			item.type = CodeItem.TYPE_CSS;

			final String prop = (String) keys[j];
			item.code = prop;
			item.codeForDoc = item.code;
			item.codeDisplay = prop;
			item.codeLowMatch = prop.toLowerCase();
			item.isCSSProperty = true;
			item.fmClass = CodeItem.FM_CLASS_CSS;

			synchronized (cssCodeItems) {
				cssCodeItems.add(item);
			}
		}

		codeHelper.window.codeInvokeCounter.initCounter(cssCodeItems);
	}

	final HashMap<String, String> getProperties() {
		synchronized (cssProperties) {
			final HashMap<String, String> props = cssProperties;
			if (props.size() != 0) {
				return props;
			}

			props.put("azimuth", "aural.html");
			props.put("background", "colors.html");
			props.put("background-attachment", "colors.html");
			props.put("background-color", "colors.html");
			props.put("background-image", "colors.html");
			props.put("background-position", "colors.html");
			props.put("background-repeat", "colors.html");
			props.put("border", "box.html");
			props.put("border-bottom", "box.html");
			props.put("border-bottom-color", "box.html");
			props.put("border-bottom-style", "box.html");
			props.put("border-bottom-width", "box.html");
			props.put("border-collapse", "tables.html");
			props.put("border-color", "box.html");
			props.put("border-left", "box.html");
			props.put("border-left-color", "box.html");
			props.put("border-left-style", "box.html");
			props.put("border-left-width", "box.html");
			props.put("border-right", "box.html");
			props.put("border-right-color", "box.html");
			props.put("border-right-style", "box.html");
			props.put("border-right-width", "box.html");
			props.put("border-spacing", "tables.html");
			props.put("border-style", "box.html");
			props.put("border-top", "box.html");
			props.put("border-top-color", "box.html");
			props.put("border-top-style", "box.html");
			props.put("border-top-width", "box.html");
			props.put("border-width", "box.html");
			props.put("bottom", "visuren.html");
			props.put("caption-side", "tables.html");
			props.put("clear", "visuren.html");
			props.put("clip", "visufx.html");
			props.put("color", "colors.html");
			props.put("content", "generate.html");
			props.put("counter-increment", "generate.html");
			props.put("counter-reset", "generate.html");
			props.put("cue", "aural.html");
			props.put("cue-after", "aural.html");
			props.put("cue-before", "aural.html");
			props.put("cursor", "ui.html");
			props.put("direction", "visuren.html");
			props.put("display", "visuren.html");
			props.put("elevation", "aural.html");
			props.put("empty-cells", "tables.html");
			props.put("float", "visuren.html");
			props.put("font", "fonts.html");
			props.put("font-family", "fonts.html");
			props.put("font-size", "fonts.html");
			props.put("font-style", "fonts.html");
			props.put("font-variant", "fonts.html");
			props.put("font-weight", "fonts.html");
			props.put("height", "visudet.html");
			props.put("left", "visuren.html");
			props.put("letter-spacing", "text.html");
			props.put("line-height", "visudet.html");
			props.put("list-style", "generate.html");
			props.put("list-style-image", "generate.html");
			props.put("list-style-position", "generate.html");
			props.put("list-style-type", "generate.html");
			props.put("margin", "box.html");
			props.put("margin-bottom", "box.html");
			props.put("margin-left", "box.html");
			props.put("margin-right", "box.html");
			props.put("margin-top", "box.html");
			props.put("max-height", "visudet.html");
			props.put("max-width", "visudet.html");
			props.put("min-height", "visudet.html");
			props.put("min-width", "visudet.html");
			props.put("orphans", "page.html");
			props.put("outline", "ui.html");
			props.put("outline-color", "ui.html");
			props.put("outline-style", "ui.html");
			props.put("outline-width", "ui.html");
			props.put("overflow", "visufx.html");
			props.put("padding", "box.html");
			props.put("padding-bottom", "box.html");
			props.put("padding-left", "box.html");
			props.put("padding-right", "box.html");
			props.put("padding-top", "box.html");
			props.put("page-break-after", "page.html");
			props.put("page-break-before", "page.html");
			props.put("page-break-inside", "page.html");
			props.put("pause", "aural.html");
			props.put("pause-after", "aural.html");
			props.put("pause-before", "aural.html");
			props.put("pitch", "aural.html");
			props.put("pitch-range", "aural.html");
			props.put("play-during", "aural.html");
			props.put("position", "visuren.html");
			props.put("quotes", "generate.html");
			props.put("richness", "aural.html");
			props.put("right", "visuren.html");
			props.put("speak", "aural.html");
			props.put("speak-header", "aural.html");
			props.put("speak-numeral", "aural.html");
			props.put("speak-punctuation", "aural.html");
			props.put("speech-rate", "aural.html");
			props.put("stress", "aural.html");
			props.put("table-layout", "tables.html");
			props.put("text-align", "text.html");
			props.put("text-decoration", "text.html");
			props.put("text-indent", "text.html");
			props.put("text-transform", "text.html");
			props.put("top", "visuren.html");
			props.put("unicode-bidi", "visuren.html");
			props.put("vertical-align", "visudet.html");
			props.put("visibility", "visufx.html");
			props.put("voice-family", "aural.html");
			props.put("volume", "aural.html");
			props.put("white-space", "text.html");
			props.put("widows", "page.html");
			props.put("width", "visudet.html");
			props.put("word-spacing", "text.html");
			props.put("z-index", "visuren.html");

			buildPropCodeItems(props);

			return props;
		}
	}

	final String getDocs(final String prop) {
		synchronized (cssDocs) {
			final String doc = cssDocs.get(prop);
			if (doc != null) {
				return doc;
			}
		}

		return processDoc(prop);
	}

	private final String processDoc(final String prop) {
		final HashMap<String, String> map = getProperties();
		final String fileName = map.get(prop);
		if (fileName == null) {
			LogManager.errToLog("[CSS] property [" + prop + "] is not exits!");
			return null;
		}

		final String data = CSSHelper.loadData(ResourceUtil.getResourceAsStream(buildCSSFilePath(fileName)));
		synchronized (cssDocs) {
			CSSHelper.readCssDoc(data, cssDocs);
			return cssDocs.get(prop);
		}
	}

	public final void resetCache() {
		synchronized (cache) {
			cache.clear();
		}
		synchronized (cssCodeItems) {
			CodeItem.cycle(cssCodeItems);
		}
		synchronized (cssDocs) {
			cssDocs.clear();
		}
		synchronized (cssProperties) {
			cssProperties.clear();
		}
	}

	static final String bodyRule = "body { font-family:Dialog, Arial, Helvetica, sans-serif; font-size: "
			+ DefaultManager.getDesignerDocFontSize() + "pt; }";// font-family:
																																										// " +
																																										// font.getFamily()
																																										// + ";
	static final String strongRule = ".strong { font-weight:bold; font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";// font-family:
																																	// " +
																																	// font.getFamily()
																																	// + ";
	static final String aRule = "a { font-family:Dialog, Arial, Helvetica, sans-serif; text-decoration:underline; color:blue; font-size: "
			+ DefaultManager.getDesignerDocFontSize() + "pt; }";// font-family:
																																																		// " +
																																																		// font.getFamily()
																																																		// + ";
	static final String codeRule = "code { font-size: " + DefaultManager.getDesignerDocFontSize() + "pt; }";// font-family: " + font.getFamily() + ";
	static final String preRule = "pre { font-style:italic; font-family:Dialog, Arial, Helvetica, sans-serif; font-size: "
			+ DefaultManager.getDesignerDocFontSize() + "pt; }";// font-family:
																																														// " +
																																														// font.getFamily()
																																														// + ";
	static final String rubyMethodRule = ".method-callseq {font-weight:bold; font-size: " + DefaultManager.getDesignerDocFontSize()
			+ "pt;}";
	static final String background_color = "#FAFBC5";
	static final Color bg_color = Color.decode(background_color);

	private JFrame codeFrame;
	private DocLayoutLimit layoutLimit;
	private final JEditorPane docPane = new JEditorPane();
	private final JScrollPane scrollPanel = new JScrollPane(docPane);// 不能使用NEVER，因为内容可能含图片
	private final JFrame docFrame = new JFrame("");
	public final CodeHelper codeHelper;
	public boolean isForMouseOverTip;
	public int mouseOverX, mouseOverY, mouseOverFontHeight;
	private CodeItem currItem;

	public DocHelper(final CodeHelper codeHelper) {
		this.codeHelper = codeHelper;

		docPane.setBorder(new EmptyBorder(4, 4, 4, 4));
		docPane.setBackground(bg_color);
		docPane.setContentType("text/html");
		final StyleSheet styleSheet = ((HTMLDocument) docPane.getDocument()).getStyleSheet();

		CodeHelper.buildListenerForScroll(scrollPanel, codeHelper);

		docPane.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				codeHelper.flipTipKeepOn();
			}

			@Override
			public void mouseDragged(final MouseEvent e) {
			}
		});

		docPane.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}

			@Override
			public void mousePressed(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				// codeHelper.flipTipStop();
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				// codeHelper.flipTipKeepOn();
			}

			final HTMLDocument hDoc = (HTMLDocument) docPane.getDocument();
			final String classStartType1 = "../";
			final String classStartType2 = "/api/";
			final char methodSpliter = '#';
			final int methodSpliterLength = 1;
			final char parameterSpliter = '-';
			final String strParameterSpliter = String.valueOf(parameterSpliter);
			final int parameterSpliterLength = 1;
			final Pattern removePackageName = Pattern.compile("\\b(\\w+\\.)");
			final ClassLoader classLoader = DocHelper.class.getClassLoader();

			private static final String PROPDEF = "propdef-";

			private final void processCssDoc(final SimpleAttributeSet value) {
				final String href = (String) value.getAttribute(HTML.Attribute.HREF);
				if (href != null) {// media.html#visual-media-group 或
									// colors.html#propdef-background-color
					try {
						final int sectIdx = href.indexOf('#');
						final boolean hasSect = sectIdx >= 0;
						// final String fileName = hasSect?href.substring(0,
						// sectIdx):href;
						if (hasSect) {
							final String fragment = href.substring(sectIdx + 1);
							if (fragment.startsWith(PROPDEF)) {
								final String propName = fragment.substring(PROPDEF.length());
								final String doc = getDocs(propName);
								if (doc != null) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											loadDoc(doc);
										}
									});
									return;
								}
							}
						}
						HttpUtil.browse(CSSHelper.getCSSDocWebURL(href));
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
					// LogManager.log("click href : " + href);
				}
			}

			/**
			 * 从一个java doc页面跳到另一个java doc页面
			 */
			@Override
			public void mouseClicked(final MouseEvent e) {
				final int pos = docPane.viewToModel(e.getPoint());
				if (pos >= 0) {
					final javax.swing.text.Element element = hDoc.getCharacterElement(pos);
					final AttributeSet a = element.getAttributes();

					final SimpleAttributeSet value = (SimpleAttributeSet) a.getAttribute(HTML.Tag.A);
					if (value != null) {
						if (currItem.isCSSProperty) {
							processCssDoc(value);
						} else {
							processJavaDoc(value);
						}
					}
				}
			}

			final void processJavaDoc(final SimpleAttributeSet value) {
				final String href = (String) value.getAttribute(HTML.Attribute.HREF);
				String doc = null;

				try {
					if (href != null) {
						// ../../javax/swing/JComponent.html#getAutoscrolls--
						// click href : ../../../hc/server/ui/HTMLMlet.html
						// click href :
						// http://docs.oracle.com/javase/8/docs/api/javax/swing/JComponent.html?is-external=true
						// click href :
						// ../../../hc/server/ui/Mlet.html#resizeImage(java.awt.image.BufferedImage,
						// int, int)
						// click href :
						// ../../../hc/server/ui/ProjectContext.html#getMobileOS()
						// click href :
						// ../../java/awt/Toolkit.html#createCustomCursor-java.awt.Image-java.awt.Point-java.lang.String-
						if (currItem.isRubyClass) {
							// Numeric.html#method-i-divmod
							final String shortClassName = href.substring(0, href.indexOf('.', 0));
							int rubyMethodIdx = href.indexOf(ruby_method_i, 0);
							boolean isStatic = false;
							final String methodName;
							if (rubyMethodIdx > 0) {
								methodName = href.substring(rubyMethodIdx + ruby_method_i.length());
							} else {
								rubyMethodIdx = href.indexOf(ruby_method_c, 0);
								if (rubyMethodIdx > 0) {
									isStatic = true;
									methodName = href.substring(rubyMethodIdx + ruby_method_c.length());
								} else {
									methodName = DOC_CLASS_DESC_KEY;
								}
							}

							final RubyClassAndDoc rcd = RubyHelper.searchRubyClassByShortName(shortClassName);
							if (rcd == null) {
								return;
							}

							final String fullClassName = rcd.claz.getName();
							if (contains(fullClassName) == false) {
								processDocForOneLevel(codeHelper, rcd.claz, fullClassName);
							}

							doc = getDoc(fullClassName, (isStatic ? (RubyHelper.RUBY_STATIC_MEMBER_DOC + methodName) : methodName));
							return;
						}

						final int classEndIdx = href.lastIndexOf(".html");
						if (classEndIdx >= 0) {
							try {
								int classStartIdx = href.lastIndexOf(classStartType1);
								if (classStartIdx >= 0) {
									classStartIdx += classStartType1.length();
								} else {
									classStartIdx = href.lastIndexOf(classStartType2);
									if (classStartIdx >= 0) {
										classStartIdx += classStartType2.length();
									}
								}

								final String preClass = href.substring(classStartIdx, classEndIdx);
								final String fullClassName = preClass.replace('.', '$').replace('/', '.');// java/util/Map.Entry =>
																											// java.util.Map$Entry

								// if(fullClassName.equals(CodeHelper.J2SE_STRING_CLASS.getName())){//有响应感，仅管无实用
								// return;
								// }

								String fieldOrMethodName = DOC_CLASS_DESC_KEY;
								final int methodSplitIdx = href.indexOf(methodSpliter, classEndIdx);
								if (methodSplitIdx >= 0) {
									final String methodPart = href.substring(methodSplitIdx + methodSpliterLength);
									final int firstParameterSpliterIdx = methodPart.indexOf(parameterSpliter);
									if (firstParameterSpliterIdx < 0) {
										fieldOrMethodName = removePackageName.matcher(methodPart).replaceAll("");
									} else {
										final String methodName = methodPart.substring(0, firstParameterSpliterIdx);
										String parameter = methodPart.substring(firstParameterSpliterIdx + parameterSpliterLength,
												methodPart.length() - parameterSpliterLength);
										parameter = parameter.replaceAll(strParameterSpliter, ", ");
										parameter = removePackageName.matcher(parameter).replaceAll("");
										fieldOrMethodName = methodName + "(" + parameter + ")";
									}

									final int classIdx = fullClassName.lastIndexOf('.');
									if (classIdx > 0) {
										final String className = fullClassName.substring(classIdx + 1);
										if (fieldOrMethodName.startsWith(className)) {// 将构造方法转为new()
											fieldOrMethodName = CodeHelper.JRUBY_NEW + fieldOrMethodName.substring(className.length());
										}
									}
								}

								try {
									CodeHelper.buildForClass(codeHelper, Class.forName(fullClassName, false, classLoader));
								} catch (final ClassNotFoundException e1) {
								}

								doc = getDoc(fullClassName, fieldOrMethodName);
							} catch (final Exception e) {
							}
						}
					}
				} finally {
					if (doc != null && doc.length() > 0) {
						final String dispDoc = doc;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								loadDoc(dispDoc);
							}
						});
					} else {
						if (href.startsWith("http")) {
							HttpUtil.browse(href);
						}
					}
				}
			}
		});

		styleSheet.addRule(bodyRule);
		styleSheet.addRule(strongRule);
		styleSheet.addRule(aRule);
		styleSheet.addRule(preRule);
		styleSheet.addRule(codeRule);
		styleSheet.addRule(rubyMethodRule);

		docPane.setEditable(false);
		docFrame.setVisible(false);
		docFrame.setAlwaysOnTop(true);
		docFrame.setFocusableWindowState(false);
		docFrame.setUndecorated(true);

		docFrame.getContentPane().add(scrollPanel);
		docFrame.setPreferredSize(new Dimension(CodeWindow.MAX_WIDTH, CodeWindow.MAX_HEIGHT));
		docFrame.pack();
	}

	private static String buildExtendsRubyClassHref(final Class rubyClass) {
		// Numeric.html#method-i-divmod
		final RubyClassAndDoc rcd = RubyHelper.searchRubyClass(rubyClass);
		if (rcd != null) {
			return " extends <a href=\"" + rcd.docShortName + ".html\">" + rcd.docShortName + "</a>";
		}
		return null;
	}

	final Dimension docFrameSize = new Dimension();
	final Dimension codeFrameSize = new Dimension();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	private final Runnable repainRunnable = new Runnable() {
		@Override
		public void run() {
			// docPane.invalidate();
			// scrollPanel.invalidate();
			// docFrame.validate();
			// ClassUtil.revalidate(scrollPanel);
			// ClassUtil.revalidate(docFrame);
			// docFrame.pack();

			docFrame.getSize(docFrameSize);

			int showX, showY;
			if (isForMouseOverTip) {
				showX = mouseOverX;
				showY = mouseOverY + mouseOverFontHeight;
				if (showY + docFrameSize.height > screenSize.height) {
					showY = mouseOverY - docFrameSize.height;
				}
				if (showX + docFrameSize.width > screenSize.width) {
					showX = mouseOverX - docFrameSize.width;
				}
			} else {
				codeFrame.getSize(codeFrameSize);
				showX = codeFrame.getX();
				showY = codeFrame.getY();
				if (showX + codeFrameSize.width + docFrameSize.width > screenSize.width) {
					if (showX - docFrameSize.width < 0) {
						if (layoutLimit.isNotUpLayout == false && (layoutLimit.isNotDownLayout
								|| showY + codeFrameSize.height + docFrameSize.height > screenSize.height)) {
							showY -= docFrameSize.height;// 上置
						} else {
							showY += codeFrameSize.height;// 下置
						}
					} else {
						showX -= docFrameSize.width;// 左置
					}
				} else {
					showX += codeFrameSize.width;// 自然右置
				}
			}
			docFrame.setLocation(showX, showY);
			docFrame.setVisible(true);
			SwingUtilities.invokeLater(scrollToTopRun);
			if (L.isInWorkshop) {
				LogManager.log("[CodeTip] docFrame.setVisible(true).");
			}
		}
	};

	public final void release() {
		docFrame.dispose();
	}

	public final boolean isShowing() {
		return docFrame.isVisible();
	}

	public final void setInvisible() {
		docFrame.setVisible(false);
		if (L.isInWorkshop) {
			ClassUtil.printCurrentThreadStack("[CodeTip] docFrame.setVisible(false)");
		}
	}

	public final void setCurrItemForTest(final CodeItem item) {
		currItem = item;
	}

	public final void popDocTipWindow(final CodeItem item, final JFrame codeWindow, String fmClass, String fieldOrMethodName,
			final int type, final DocLayoutLimit layoutLimit) {
		final boolean isForClassDoc = (type == CodeItem.TYPE_CLASS);

		// 支持类的doc描述
		fmClass = isForClassDoc ? fieldOrMethodName : fmClass;
		if (fmClass == null) {
			setInvisible();
		}

		fieldOrMethodName = isForClassDoc ? DOC_CLASS_DESC_KEY : fieldOrMethodName;

		this.codeFrame = codeWindow;
		this.layoutLimit = layoutLimit;
		this.currItem = item;

		final String doc = getDoc(fmClass, fieldOrMethodName);
		if (doc != null && doc.length() > 0) {
			loadDoc(doc);
			SwingUtilities.invokeLater(repainRunnable);
		} else {
			if (L.isInWorkshop) {
				LogManager.log("[CodeTip] fail to get doc and setInvisible() about : " + fmClass + "/" + fieldOrMethodName);
			}
			setInvisible();
		}
	}

	public final boolean acceptType(final int type) {
		return type == CodeItem.TYPE_FIELD || type == CodeItem.TYPE_METHOD || type == CodeItem.TYPE_CLASS || type == CodeItem.TYPE_CSS;
	}

	private final boolean contains(final String claz) {
		synchronized (cache) {
			return cache.get(claz) != null;
		}
	}

	/**
	 * 如果没有相应文档，则返回null或空串
	 * 
	 * @param claz
	 * @param fieldOrMethod
	 *            如：getFreeMessage(String)，没有实参
	 * @return
	 */
	public final String getDoc(final String claz, final String fieldOrMethod) {
		// LogManager.log("class : " + claz + ", fieldOrMethod : " +
		// fieldOrMethod);
		if (currItem.isCSSProperty) {
			return getDocs(fieldOrMethod);
		}

		synchronized (cache) {
			HashMap<String, String> map = cache.get(claz);

			if (map == null) {
				try {
					Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);// 不能采用Notify技术，因为有可能不是目标claz装入
				} catch (final Exception e) {
				}
				map = cache.get(claz);// 等待异步线程完成doc内容
				if (map == null) {
					return null;
				}
			}

			return map.get(fieldOrMethod);
		}
	}

	private final void loadDoc(final String doc) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append("<html><body style=\"background-color:").append(background_color).append("\">").append(doc).append("</body></html>");
		docPane.setText(sb.toString());
		StringBuilderCacher.cycle(sb);
		SwingUtilities.invokeLater(scrollToTopRun);
	}

	final Runnable scrollToTopRun = new Runnable() {
		@Override
		public void run() {
			scrollPanel.getVerticalScrollBar().setValue(0);
		}
	};

	static String buildCSSFilePath(final String fileName) {
		return CSSHelper.CSS_BASE + fileName;
	}

	public static final URL getCSSResource(final String fileName) {
		return ResourceUtil.getResource(DocHelper.buildCSSFilePath(fileName));
	}

	public final void processDoc(final CodeHelper codeHelper, final Class c, final boolean processInDelay) {
		final String clasName = c.getName();
		synchronized (cache) {
			if (cache.containsKey(clasName)) {
				return;
			}
		}
		// if(processInDelay){
		// ContextManager.getThreadPool().run(new Runnable() {
		// @Override
		// public void run() {
		// processDoc(codeHelper, c, clasName, false);
		// }
		// });
		// }else{
		processDoc(codeHelper, c, clasName, false);
		// }
	}

	private final void processDocForOneLevel(final CodeHelper codeHelper, final Class c, final String clasName) {
		// System.out.println("-----processDocForOneLevel : " + claz.getName());

		final RubyClassAndDoc clazDoc = RubyHelper.searchRubyClass(c);
		if (clazDoc != null) {
			read(c, codeHelper, true, clasName,
					ResourceUtil.getResourceAsStream("hc/res/docs/ruby/Ruby" + clazDoc.docShortName + "2_2_0.htm"));// RubyString2_2_0.htm
			final Class rubyParent = clazDoc.claz.getSuperclass();
			if (RubyHelper.searchRubyClass(rubyParent) != null) {
				processDoc(codeHelper, rubyParent, false);
			}
			return;
		}

		final String className = clasName.replace('.', '/');

		final String fileName;
		if (className.indexOf('$') > 0) {
			fileName = className.replace('$', '.');// java.util.Map$Entry =>
													// java.util.Map.Entry
		} else {
			fileName = className;
		}

		if (clasName.startsWith(CodeHelper.JAVA_PACKAGE_CLASS_PREFIX)) {
			read(c, codeHelper, false, clasName, J2SEDocHelper.getDocStream(buildClassDocPath(fileName)));
		} else if (clasName.startsWith(CodeHelper.HC_PACKAGE_CLASS_PREFIX)) {
			read(c, codeHelper, false, clasName, ResourceUtil.getResourceAsStream("hc/res/docs/" + fileName + ".html"));
		}
	}

	public static String buildClassDocPath(final String clasName) {
		return "hc/res/docs/jdk_docs/api/" + clasName + ".html";
	}

	private final void read(final Class c, final CodeHelper codeHelper, final boolean isRubyClass, final String clasName,
			final InputStream in) {
		if (in == null) {
			if (J2SEDocHelper.isBuildIn() == false && J2SEDocHelper.isJ2SEDocReady() == false) {
				return;
			}

			synchronized (cache) {
				cache.put(clasName, new HashMap<String, String>());
			}
			return;
		}
		final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		final int BUFFER_SIZE = 4096;
		final byte[] data = new byte[BUFFER_SIZE];
		int count = -1;
		try {
			while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
				outStream.write(data, 0, count);
			final String docContent = new String(outStream.toByteArray(), IConstant.UTF_8);
			toCache(c, codeHelper, isRubyClass, clasName, docContent);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			synchronized (cache) {
				cache.put(clasName, new HashMap<String, String>());
			}
		}
		return;
	}

	private static final int GROUP_IDX = 2;
	private static final String blockListLast = "<ul class=\"blockListLast\">";
	private static final Pattern jianKuoHao = Pattern.compile("\\<(.*?)\\>");
	private static final Pattern generics_e_left = Pattern.compile("&lt;");
	private static final Pattern generics_e_right = Pattern.compile("&gt;");
	private static final Pattern generics_e_type = Pattern.compile("&lt;(.*?)&gt;");// 不能(.*)
	private static final Pattern generics_e_to_object_pattern = Pattern.compile("\\b([A-Z]{1})\\b");
	private static final Pattern blockPattern = Pattern
			.compile("<ul class=\"(blockListLast|blockList)\">\n<li class=\"blockList\">\n(.*?)</li>\n</ul>\n", Pattern.DOTALL);
	private static final Pattern fieldOrMethodNamePattern = Pattern.compile("<pre>(.*?)</pre>", Pattern.DOTALL);// javaDoc method throws Exception有换行现象
	private static final Pattern classDescPattern = Pattern
			.compile("<div class=\"description\">\n<ul class=\"blockList\">\n<li class=\"blockList\">\n" + // <hr>\n<br>\n
					"(.*?)" + "\n</li>\n</ul>\n</div>\n", Pattern.DOTALL);
	private static final Pattern rubyStringMethodsList = Pattern
			.compile("<div id=\"method-list-section\" class=\"section\">" + "(.*?)" + "</div>", Pattern.DOTALL);
	private static final Pattern rubyConstantsList = Pattern
			.compile("<h3 class=\"section-header\">Constants</h3>\\s*" + "<dl>" + "(.*?)" + "</dl>", Pattern.DOTALL);
	private static final Pattern rubyConstantsItem = Pattern
			.compile("<dt><a name=\"(.*?)\">(.*?)</a></dt>\\s*<dd class=\"description\">(.*?)</dd>", Pattern.DOTALL);

	private static final Pattern rubyStringMethodItem = Pattern.compile("<li><a href=\"#(.*?)\">(#|::)(.*?)</a></li>");
	private static final Pattern rubyReturnTypeEnumeratorPattern = Pattern
			.compile("<span class=\"method-callseq\">(.*?)(&rarr;|=>) an_enumerator</span>");
	private static final Pattern rubyReturnTypePattern = Pattern.compile("<span class=\"method-callseq\">(.*?)</span>");
	private static final String ruby_method_i = "method-i-";
	private static final String ruby_method_c = "method-c-";
	private static final String[] arrowType = { "&rarr; ", " => " };

	public static final String DOC_CLASS_DESC_KEY = "DOC_CLASS_DESC_KEY";

	public static final String NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME = "";

	private static void processRubyDoc(final Class c, final HashMap<String, String> docMap, final String docContent,
			final String simpleClassName) {
		final RubyClassAndDoc rcd = RubyHelper.searchRubyClass(c);
		final Class superClass = c.getSuperclass();
		final String href = buildExtendsRubyClassHref(superClass);
		final String rubyDocPre = "<STRONG>Ruby Doc</STRONG><BR><BR>" + rcd.docShortName + (href == null ? "" : href) + "<BR>";

		{
			final String classStartTag = "<div id=\"description\" class=\"description\">";
			int startIdx = docContent.indexOf(classStartTag, 0);
			if (startIdx > 0) {
				final String classEndTag = "</div><!-- description -->";
				final int endIdx = docContent.indexOf(classEndTag, startIdx);
				if (endIdx > 0) {
					startIdx += classStartTag.length();
					docMap.put(DOC_CLASS_DESC_KEY, rubyDocPre + docContent.substring(startIdx, endIdx));
				}
			}
		}

		final RubyClassAndDoc classAndDoc = RubyHelper.searchRubyClass(c);
		final boolean isMethodsInited = classAndDoc.isInitMethods;

		// Constants
		{
			final Matcher matcher = rubyConstantsList.matcher(docContent);
			if (matcher.find()) {
				// <dt><a name="CLOCK_BOOTTIME">CLOCK_BOOTTIME</a></dt>
				// <dd class="description"></dd>
				final Matcher itemMatcher = rubyConstantsItem.matcher(matcher.group());
				while (itemMatcher.find()) {
					final String item_c = itemMatcher.group(2);
					final String item_desc = itemMatcher.group(3);

					addDocItem(docMap, rubyDocPre, true, item_c, item_desc);

					if (isMethodsInited == false) {
						final RubyMethodItem methodItem = new RubyMethodItem(item_c, true, null, c);
						methodItem.setConstant(true);
						classAndDoc.staticMethods.add(methodItem);// 含constants
					}
				}
			}
		}

		// final int publicInstanceMethodIdx = docContent.indexOf("<h3
		// class=\"section-header\">Public Instance Methods</h3>");
		final int privateInstanceMethodIdx = docContent.indexOf("<h3 class=\"section-header\">Private Instance Methods</h3>");

		final Matcher matcher = rubyStringMethodsList.matcher(docContent);
		if (matcher.find()) {
			final String removeTail = "<div class=\"method-source-code\"";

			final String match = matcher.group();
			final Matcher itemMatcher = rubyStringMethodItem.matcher(match);
			final Vector<RubyAliasMethod> aliasMethods = new Vector<RubyAliasMethod>();

			while (itemMatcher.find()) {
				final String type = itemMatcher.group(2);
				final boolean isStatic = type.equals("::");
				final String rubyMethodTag = isStatic ? ruby_method_c : ruby_method_i;
				final String locateID = itemMatcher.group(1);
				final String fieldOrMethodName = itemMatcher.group(3);

				final String startStr = "<a name=\"" + locateID + "\"></a>";
				final String endStr = "</div><!-- " + locateID.substring(rubyMethodTag.length()) + "-method -->";

				final int cutStartIdx = docContent.indexOf(startStr) + startStr.length();
				final int cutEndIdx = docContent.indexOf(removeTail, cutStartIdx);
				final int cutMaxEndIdx = docContent.indexOf(endStr, cutStartIdx);

				final boolean isPublic = isStatic ? true : (!(0 < privateInstanceMethodIdx && privateInstanceMethodIdx < cutStartIdx));

				String doc = docContent.substring(cutStartIdx, (cutMaxEndIdx < cutEndIdx || cutEndIdx == -1) ? cutMaxEndIdx : cutEndIdx);
				final String firstPart = "<span class=\"method-click-advice\">click to toggle source</span>";// 相同的方法可能有多个同名，此段
																												// 只
																												// 用于第一个之后
				final String docFinalDetail;
				{
					final int idx = doc.indexOf(firstPart, 0);
					if (idx > 0) {
						docFinalDetail = doc.substring(idx + firstPart.length());
						doc = doc.replace(firstPart, "");// JRUBY_FILE_CLASS
					} else {
						docFinalDetail = doc;
					}
				}
				final String returnType = searchRubyReturnType(doc);
				// System.out.println("\n\nruby method : " + fieldOrMethodName +
				// "\n" + doc);
				addDocItem(docMap, rubyDocPre, isStatic, fieldOrMethodName, doc);
				if (isMethodsInited == false) {
					final RubyMethodItem methodItem = new RubyMethodItem(fieldOrMethodName, isPublic, returnType, c);
					if (isStatic) {
						classAndDoc.staticMethods.add(methodItem);// 含constants
					} else {
						classAndDoc.insMethods.add(methodItem);
					}
					searchAliasMethods(aliasMethods, fieldOrMethodName, docFinalDetail, methodItem, isStatic, c);
				}
			}

			if (aliasMethods.size() > 0) {
				for (int i = 0; i < aliasMethods.size(); i++) {
					final RubyAliasMethod method = aliasMethods.get(i);
					final Vector<RubyMethodItem> list = method.isStatic ? classAndDoc.staticMethods : classAndDoc.insMethods;
					if (containsMethod(list, method.methodName) == false) {
						list.add(method.build());
					}
				}
			}

		}

		if (isMethodsInited == false) {
			classAndDoc.isInitMethods = true;
		}
	}

	private static void addDocItem(final HashMap<String, String> docMap, final String rubyDocPre, final boolean isStatic,
			final String fieldOrMethodName, final String doc) {
		docMap.put((isStatic ? RubyHelper.RUBY_STATIC_MEMBER_DOC : "") + fieldOrMethodName, rubyDocPre + "<BR>" + doc);
	}

	private final static char[] removePartStart = { '(', '{', '[' };

	private final static void searchAliasMethods(final Vector<RubyAliasMethod> aliasMethods, final String fieldOrMethodName,
			final String doc, final RubyMethodItem methodItem, final boolean isStatic, final Class c) {
		final Matcher returnMatcher = rubyReturnTypePattern.matcher(doc);// <span
																			// class="method-callseq">now
																			// &rarr;
																			// time</span>
		while (returnMatcher.find()) {
			String searchAlias = returnMatcher.group(1);
			{
				final int size = removePartStart.length;
				for (int i = 0; i < size; i++) {
					final int idx = searchAlias.indexOf(removePartStart[i]);
					if (idx >= 0) {
						searchAlias = searchAlias.substring(0, idx);
					}
				}

			}
			for (int i = 0; i < arrowType.length; i++) {
				final String arraw = arrowType[i];
				final int idx = searchAlias.indexOf(arraw);
				if (idx >= 0) {
					searchAlias = searchAlias.substring(0, idx);
				}
			}
			searchAlias = searchAlias.trim();
			if (searchAlias.length() == 0) {
				continue;
			} else if (searchAlias.startsWith("`")) {// [`cmd`] at
														// hc.server.ui.design.code.JRubyKernel
				continue;
			} else if (searchAlias.indexOf("::", 0) > 0) {// [Kernel::abort] at
															// hc.server.ui.design.code.JRubyKernel
				continue;
			} else if (searchAlias.indexOf(" " + fieldOrMethodName + " ", 0) > 0) {// ary
																					// <=>
																					// other_ary
				continue;
			}

			if (unchar.matcher(searchAlias).find()) {
				L.V = L.WShop ? false : LogManager.log("one alias : [" + searchAlias + "] at " + c.getName() + "\n" + doc);
			}
			aliasMethods.add(new RubyAliasMethod(searchAlias, fieldOrMethodName, isStatic, methodItem));
		}
	}

	private final static Pattern unchar = Pattern.compile("[^a-zA-Z0-9_?=!]");

	private final static boolean containsMethod(final Vector<RubyMethodItem> list, final String methodName) {
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			if (list.get(i).methodOrField.equals(methodName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * null means not found
	 * 
	 * @param doc
	 * @return
	 */
	private static String searchRubyReturnType(final String doc) {
		if (rubyReturnTypeEnumeratorPattern.matcher(doc).find()) {// <span
																	// class="method-callseq">each_byte
																	// &rarr;
																	// an_enumerator</span>
			return RubyHelper.ENUMERATOR;
		}

		final Matcher returnMatcher = rubyReturnTypePattern.matcher(doc);// <span
																			// class="method-callseq">now
																			// &rarr;
																			// time</span>
		if (returnMatcher.find()) {
			final String returnDetail = returnMatcher.group(1);
			return searchRubyReturnTypeForResult(returnDetail);
		}
		return null;
	}

	private static String searchRubyReturnTypeForResult(final String returnDetail) {
		for (int i = 0; i < arrowType.length; i++) {
			final String arrow = arrowType[i];
			final int idx = returnDetail.indexOf(arrow);
			if (idx >= 0) {
				return returnDetail.substring(idx + arrow.length());
			}
		}

		return null;
	}

	private static void processDoc(final CodeHelper codeHelper, final String clasName, final HashMap<String, String> docMap,
			final String docContent, final String simpleClassName) {
		{
			final Matcher matcher = classDescPattern.matcher(docContent);
			if (matcher.find()) {
				String doc = matcher.group(1);
				final String hr = "<hr>\n<br>\n";
				final int hrIdx = doc.indexOf(hr, 0);
				if (hrIdx >= 0) {
					doc = doc.substring(hrIdx + hr.length());
				}
				doc = doc.replaceFirst("<pre>", "<strong>");
				doc = doc.replaceFirst("</pre>", "</strong><BR><BR>");
				docMap.put(DOC_CLASS_DESC_KEY, doc);
			}
		}

		{
			final int constructorDocIdx = docContent.indexOf("<h3>Constructor Detail</h3>");
			if (constructorDocIdx > 0) {
				processListBlock(codeHelper, clasName, docContent.substring(constructorDocIdx), docMap, true, simpleClassName, true);
			}
		}

		{
			final int detailDocIdx = docContent.indexOf("<h3>Field Detail</h3>");
			if (detailDocIdx > 0) {
				processListBlock(codeHelper, clasName, docContent.substring(detailDocIdx), docMap, false, simpleClassName, false);
			}
		}

		{
			final int detailDocIdx = docContent.indexOf("<h3>Method Detail</h3>");
			if (detailDocIdx > 0) {
				processListBlock(codeHelper, clasName, docContent.substring(detailDocIdx), docMap, true, simpleClassName, false);
			}
		}
	}

	private static void processListBlock(final CodeHelper codeHelper, final String clasName, final String docContent,
			final HashMap<String, String> docMap, final boolean isForMethod, final String simpleClassName, final boolean isForConstruct) {
		{
			final Matcher matcher = blockPattern.matcher(docContent);
			while (matcher.find()) {
				final String match = matcher.group(GROUP_IDX);
				processItem(codeHelper, clasName, docMap, match, isForMethod, simpleClassName, isForConstruct);
				if (matcher.group().startsWith(blockListLast)) {
					return;
				}
			}
		}
	}

	/**
	 * (java.lang.String hello, int j, boolean[] yes) => (hello, j, yes) () => ()
	 * 
	 * @param method
	 * @return
	 */
	public static String buildCodeParameterList(String parameter) {
		parameter = StringUtil.replaceBound(parameter, '<', '>', "");// (Map<?
																		// extends
																		// K, ?
																		// extends
																		// V>
																		// map)=>
																		// (Map
																		// map)
		parameter = parameter.replaceAll(", [\\w\\.\\[\\]]+ ", ", ");// method(java.lang.String
																		// hello,
																		// boolean[]
																		// yes)
																		// =>
																		// method(String,
																		// boolean
																		// yes)
		parameter = parameter.replaceAll("\\([\\w\\.\\[\\]]+ ", "(");// method(int,
																		// boolean
																		// yes)
																		// =>
																		// method(int,
																		// boolean)//有可能boolean另起一行
		parameter = parameter.replace(" ", "");// 去掉多余空格
		return parameter.replace(",", ", ");
	}

	private static void replaceCodeParameter(final CodeHelper codeHelper, final String clasName, final String methodForDoc,
			final String codeParameterList) {
		ArrayList<CodeItem> list = codeHelper.classCacheMethodAndPropForClass.get(clasName);
		if (list != null && replaceCode(clasName, methodForDoc, codeParameterList, list)) {
			return;
		}

		list = codeHelper.classCacheMethodAndPropForInstance.get(clasName);
		if (list != null) {
			replaceCode(clasName, methodForDoc, codeParameterList, list);
		}
	}

	private static boolean replaceCode(final String clasName, final String methodForDoc, final String codeParameterList,
			final ArrayList<CodeItem> list) {
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			final CodeItem item = list.get(i);
			if (methodForDoc.equals(item.codeForDoc) && item.fmClass.equals(clasName)) {
				item.code = item.fieldOrMethodOrClassName + codeParameterList;
				return true;
			}
		}
		return false;
	}

	private static void processItem(final CodeHelper codeHelper, final String clasName, final HashMap<String, String> docMap,
			final String item, final boolean isForMethod, final String simpleClassName, final boolean isForConstruct) {
		final Matcher matchFieldOrMethodName = fieldOrMethodNamePattern.matcher(item);
		if (matchFieldOrMethodName.find()) {
			String fieldOrMethodName = matchFieldOrMethodName.group(1);
			final String fieldOrMethodNameWithBR = "<strong>" + fieldOrMethodName + "</strong><BR><BR>";
			// fieldOrMethodName =
			// jianKuoHao.matcher(fieldOrMethodName).replaceAll("");
			// fieldOrMethodName =
			// generics_e_type.matcher(fieldOrMethodName).replaceAll("");
			// fieldOrMethodName =
			// generics_e_to_object_pattern.matcher(fieldOrMethodName).replaceAll(CodeHelper.OBJECT_STR);

			if (isForMethod) {
				final int kuohaoRightIdx = fieldOrMethodName.indexOf(")");
				if (kuohaoRightIdx != fieldOrMethodName.length() - 1) {
					// 右括号之后是回车，及throws XXXException
					fieldOrMethodName = fieldOrMethodName.substring(0, kuohaoRightIdx + 1);
				}
			}
			// 将参数实名去掉
			fieldOrMethodName = fieldOrMethodName.replace("&nbsp;", " ");
			fieldOrMethodName = fieldOrMethodName.replace("\n", "");
			fieldOrMethodName = douhaoSpaces.matcher(fieldOrMethodName).replaceAll(", ");// wait(int
																							// a,
																							// long
																							// ms)
																							// =>
																							// wait(int
																							// a,
																							// long
																							// ms)

			if (isForMethod) {
				if (isForConstruct && simpleClassName != NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME) {
					fieldOrMethodName = StringUtil.replaceFirst(fieldOrMethodName, simpleClassName, CodeHelper.JRUBY_NEW);// 将构造方法转为new()方法，但不将后面的参数。new
																															// Properties(Properties) =>
																															// new(new)
				}

				final int kuohaoLeftIdx = fieldOrMethodName.indexOf("(");
				String parameter = fieldOrMethodName.substring(kuohaoLeftIdx);
				parameter = jianKuoHao.matcher(parameter).replaceAll("");// 将setToolTipText()参数内"(<a
																			// href="../../java/lang/String.html"
																			// title="class
																			// in
																			// java.lang">String</a>
																			// text)"=>"(String
																			// text)"
																			// &lt;(.*?)&gt;//保留generic-type
				parameter = generics_e_left.matcher(parameter).replaceAll("<");
				parameter = generics_e_right.matcher(parameter).replaceAll(">");
				// parameter =
				// generics_e_type.matcher(parameter).replaceAll("");
				// parameter =
				// generics_e_to_object_pattern.matcher(parameter).replaceAll(CodeHelper.OBJECT_STR);
				final String codeParameterList = buildCodeParameterList(parameter);
				parameter = parameter.replaceAll("[\\w]+\\.", "");// method(java.lang.String
																	// hello,
																	// boolean
																	// yes) =>
																	// method(String
																	// hello,
																	// boolean
																	// yes)
				parameter = replaceVar(parameter, ",");// method(int hello,
														// boolean yes) =>
														// method(int, boolean
														// yes)，但不是Map<? extends
														// K, ? extends V>
				parameter = parameter.replaceAll(" \\w+\\)", ")");// method(int,
																	// boolean
																	// yes) =>
																	// method(int,
																	// boolean)//有可能boolean另起一行
				parameter = parameter.replace(" ", "");
				parameter = parameter.replace(",", ", ");
				parameter = parameter.replaceAll("extends", " extends ");
				parameter = parameter.replaceAll("super", " super ");
				final String frontPartWithName = fieldOrMethodName.substring(0, kuohaoLeftIdx);
				final int nameStartIdx = frontPartWithName.lastIndexOf(' ') + 1;

				fieldOrMethodName = frontPartWithName.substring(nameStartIdx) + parameter;

				replaceCodeParameter(codeHelper, clasName, fieldOrMethodName, codeParameterList);
			} else {
				// 去掉public final static...
				fieldOrMethodName = fieldOrMethodName.substring(fieldOrMethodName.lastIndexOf(" ") + 1);
			}

			if (docMap.containsKey(fieldOrMethodName) == false) {
				final int indexOfDoc = item.indexOf("<div class=\"block\">");
				String apiDoc = "";
				if (indexOfDoc >= 0) {
					apiDoc = item.substring(indexOfDoc);
				}
				// if(L.isInWorkshop){
				// System.out.println("item : " + fieldOrMethodName);
				// System.out.println(apiDoc + "\n\n");
				// }
				// getFreeMessage(String)
				String formatMethodDocContent = fieldOrMethodNameWithBR.replace("\n", " ").replace("&nbsp;", " ").replaceAll("[ ]{2,}",
						" ");// 消除换行和多个空格
				formatMethodDocContent = formatMethodDocContent.replaceFirst("<a (.*?)>@Deprecated</a>", "@Deprecated<BR><BR>");
				formatMethodDocContent = formatMethodDocContent.replaceAll("java\\.lang\\.String", "String");
				formatMethodDocContent = formatMethodDocContent.replaceAll("java\\.lang\\.Object", CodeHelper.OBJECT_STR);
				docMap.put(fieldOrMethodName, formatMethodDocContent + apiDoc);
			}
		}
	}

	static Pattern replaceVarPattern = Pattern.compile(" \\w+,");

	private final static String replaceVar(final String text, final String replaceTo) {
		StringBuilder sb = null;
		int copySubStrIdx = 0;
		final Matcher m = replaceVarPattern.matcher(text);
		while (m.find()) {
			final int startIdx = m.start();
			final int endIdx = m.end();

			if (sb == null) {
				sb = StringBuilderCacher.getFree();
			}

			sb.append(text.subSequence(copySubStrIdx, startIdx));

			final String mat = m.group();
			char oneChar;
			if (mat.length() == 3 && (oneChar = mat.charAt(1)) >= 'A' && oneChar <= 'Z') {
				sb.append(mat);
			} else {
				sb.append(replaceTo);
			}

			copySubStrIdx = endIdx;
		}
		if (sb == null) {
			return text;
		} else {
			sb.append(text.substring(copySubStrIdx));
			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	private final void toCache(final Class c, final CodeHelper codeHelper, final boolean isRubyClass, final String clasName,
			final String docContent) {
		final HashMap<String, String> docMap = new HashMap<String, String>();
		if (docContent != null) {
			final int classIdx = clasName.lastIndexOf(".");
			if (isRubyClass) {
				processRubyDoc(c, docMap, docContent, clasName.substring(classIdx + 1));
			} else {
				if (classIdx > 0) {
					processDoc(codeHelper, clasName, docMap, docContent, clasName.substring(classIdx + 1));
				} else {
					processDoc(codeHelper, clasName, docMap, docContent, NULL_CONSTRUCTOR_SIMPLE_CLASS_NAME);
				}
			}
		}
		synchronized (cache) {
			cache.put(clasName, docMap);
		}
	}

	/**
	 * 
	 * @param c
	 * @param clasName
	 * @param isNeedShiftToBackground
	 *            true:superAndInterface must process in background; false: current thread
	 */
	private final void processDoc(final CodeHelper codeHelper, final Class c, final String clasName,
			final boolean isNeedShiftToBackground) {
		synchronized (cache) {
			if (cache.containsKey(clasName)) {
				return;
			}
		}

		processDocForOneLevel(codeHelper, c, clasName);

		if (isNeedShiftToBackground) {
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					processSuperAndInterfaces(codeHelper, c);
				}
			});
		} else {
			processSuperAndInterfaces(codeHelper, c);
		}
	}

	private final void processSuperAndInterfaces(final CodeHelper codeHelper, final Class c) {
		final Class superclass = c.getSuperclass();
		if (superclass != null) {
			processDoc(codeHelper, superclass, superclass.getName(), false);
		}
		final Class[] interfaces = c.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			final Class claz = interfaces[i];
			processDoc(codeHelper, claz, claz.getName(), false);
		}
	}

}

class RubyAliasMethod {
	public final String methodName;
	public final String methodForDoc;
	public final RubyMethodItem rubyMethodItem;
	public final boolean isStatic;

	public RubyAliasMethod(final String name, final String docName, final boolean isStatic, final RubyMethodItem item) {
		this.methodName = name;
		this.methodForDoc = docName;
		this.isStatic = isStatic;
		this.rubyMethodItem = item;
	}

	public final RubyMethodItem build() {
		return new RubyMethodItem(methodName, methodForDoc, rubyMethodItem.isPublic, false, rubyMethodItem.returnType);
	}
}