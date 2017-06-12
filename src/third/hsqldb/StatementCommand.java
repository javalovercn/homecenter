/* Copyright (c) 2001-2016, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package third.hsqldb;

import third.hsqldb.HsqlNameManager.HsqlName;
import third.hsqldb.error.Error;
import third.hsqldb.error.ErrorCode;
import third.hsqldb.index.Index;
import third.hsqldb.lib.HashMappedList;
import third.hsqldb.lib.Iterator;
import third.hsqldb.lib.OrderedHashSet;
import third.hsqldb.lib.java.JavaSystem;
import third.hsqldb.persist.DataFileCache;
import third.hsqldb.persist.DataSpaceManager;
import third.hsqldb.persist.HsqlDatabaseProperties;
import third.hsqldb.persist.HsqlProperties;
import third.hsqldb.persist.PersistentStore;
import third.hsqldb.persist.TableSpaceManager;
import third.hsqldb.result.Result;
import third.hsqldb.result.ResultMetaData;
import third.hsqldb.rights.User;
import third.hsqldb.scriptio.ScriptWriterText;

/**
 * Implementation of Statement for SQL commands.<p>
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.3.3
 * @since 1.9.0
 */
public class StatementCommand extends Statement {

    Object[] parameters;

    StatementCommand(int type, Object[] args) {
        this(type, args, null, null);
    }

    StatementCommand(int type, Object[] args, HsqlName[] readNames,
                     HsqlName[] writeNames) {

        super(type);

        this.isTransactionStatement = true;
        this.parameters             = args;

        if (readNames != null) {
            this.readTableNames = readNames;
        }

        if (writeNames != null) {
            this.writeTableNames = writeNames;
        }

        switch (type) {

            case StatementTypes.TRUNCATE :
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                break;

            case StatementTypes.EXPLAIN_PLAN :
                group                  = StatementTypes.X_SQL_DIAGNOSTICS;
                statementReturnType    = StatementTypes.RETURN_RESULT;
                isTransactionStatement = false;
                isLogged               = false;
                break;

            case StatementTypes.DATABASE_CHECKPOINT :
                group    = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                isLogged = false;
                break;

            case StatementTypes.DATABASE_SCRIPT : {
                String name = (String) parameters[0];

                if (name == null) {
                    statementReturnType = StatementTypes.RETURN_RESULT;
                }

                group    = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                isLogged = false;

                break;
            }
            case StatementTypes.DATABASE_BACKUP :
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;

                if (writeNames.length == 0) {
                    group = StatementTypes.X_HSQLDB_NONBLOCK_OPERATION;
                }

                isLogged = false;
                break;

            case StatementTypes.SET_DATABASE_UNIQUE_NAME :
            case StatementTypes.SET_DATABASE_FILES_WRITE_DELAY :
            case StatementTypes.SET_DATABASE_FILES_TEMP_PATH :
            case StatementTypes.SET_DATABASE_FILES_EVENT_LOG :
                isTransactionStatement = false;
                group                  = StatementTypes.X_HSQLDB_SETTING;
                break;

//
            case StatementTypes.SET_DATABASE_DEFAULT_INITIAL_SCHEMA :
            case StatementTypes.SET_DATABASE_DEFAULT_TABLE_TYPE :
            case StatementTypes.SET_DATABASE_FILES_CACHE_ROWS :
            case StatementTypes.SET_DATABASE_FILES_CACHE_SIZE :
            case StatementTypes.SET_DATABASE_FILES_SCALE :
            case StatementTypes.SET_DATABASE_FILES_SPACE :
            case StatementTypes.SET_DATABASE_FILES_DEFRAG :
            case StatementTypes.SET_DATABASE_FILES_LOBS_SCALE :
            case StatementTypes.SET_DATABASE_FILES_LOBS_COMPRESSED :
            case StatementTypes.SET_DATABASE_FILES_LOG :
            case StatementTypes.SET_DATABASE_FILES_LOG_SIZE :
            case StatementTypes.SET_DATABASE_FILES_NIO :
            case StatementTypes.SET_DATABASE_FILES_SCRIPT_FORMAT :
            case StatementTypes.SET_DATABASE_AUTHENTICATION :
            case StatementTypes.SET_DATABASE_PASSWORD_CHECK :
            case StatementTypes.SET_DATABASE_PASSWORD_DIGEST :
            case StatementTypes.SET_DATABASE_PROPERTY :
            case StatementTypes.SET_DATABASE_RESULT_MEMORY_ROWS :
            case StatementTypes.SET_DATABASE_SQL_REFERENTIAL_INTEGRITY :
            case StatementTypes.SET_DATABASE_SQL :
            case StatementTypes.SET_DATABASE_TRANSACTION_CONTROL :
            case StatementTypes.SET_DATABASE_DEFAULT_ISOLATION_LEVEL :
            case StatementTypes.SET_DATABASE_TRANSACTION_CONFLICT :
            case StatementTypes.SET_DATABASE_GC :

//
            case StatementTypes.SET_DATABASE_SQL_COLLATION :
            case StatementTypes.SET_DATABASE_FILES_BACKUP_INCREMENT :
            case StatementTypes.SET_DATABASE_TEXT_SOURCE :
                group = StatementTypes.X_HSQLDB_SETTING;
                break;

            case StatementTypes.SET_DATABASE_FILES_CHECK :
                group    = StatementTypes.X_HSQLDB_SETTING;
                isLogged = false;
                break;

            case StatementTypes.SET_TABLE_CLUSTERED :
            case StatementTypes.SET_TABLE_NEW_TABLESPACE :
            case StatementTypes.SET_TABLE_SET_TABLESPACE :
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                break;

            case StatementTypes.SET_TABLE_SOURCE_HEADER :
                group    = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                isLogged = false;
                break;

            case StatementTypes.SET_TABLE_SOURCE :
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                break;

            case StatementTypes.SET_TABLE_READONLY :
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                break;

            case StatementTypes.DATABASE_SHUTDOWN :
                group = StatementTypes.X_HSQLDB_DATABASE_OPERATION;
                isTransactionStatement = false;
                isLogged               = false;
                break;

            case StatementTypes.SET_TABLE_TYPE :
                group = StatementTypes.X_HSQLDB_SCHEMA_MANIPULATION;
                break;

            case StatementTypes.SET_TABLE_INDEX :
                group                  = StatementTypes.X_HSQLDB_SETTING;
                isTransactionStatement = false;
                isLogged               = false;
                break;

            case StatementTypes.SET_USER_LOCAL :
            case StatementTypes.SET_USER_INITIAL_SCHEMA :
            case StatementTypes.SET_USER_PASSWORD :
                group                  = StatementTypes.X_HSQLDB_SETTING;
                isTransactionStatement = false;
                break;

            case StatementTypes.ALTER_SESSION :
                group                  = StatementTypes.X_HSQLDB_SESSION;
                isTransactionStatement = false;
                isLogged               = false;
                break;

            default :
                throw Error.runtimeError(ErrorCode.U_S0500,
                                         "StatementCommand");
        }
    }

    public Result execute(Session session) {

        Result result;

        try {
            result = getResult(session);
        } catch (Throwable t) {
            result = Result.newErrorResult(t, getSQL());
        }

        if (result.isError()) {
            result.getException().setStatementType(group, type);

            return result;
        }

        try {
            if (isLogged) {
                session.database.logger.writeOtherStatement(session, sql);
            }
        } catch (Throwable e) {
            return Result.newErrorResult(e, getSQL());
        }

        return result;
    }

    Result getResult(Session session) {

        if (this.isExplain) {
            return Result.newSingleColumnStringResult("OPERATION",
                    describe(session));
        }

        switch (type) {

            case StatementTypes.TRUNCATE : {
                return getTruncateResult(session);
            }
            case StatementTypes.EXPLAIN_PLAN : {
                Statement statement = (Statement) parameters[0];

                return Result.newSingleColumnStringResult("OPERATION",
                        statement.describe(session));
            }
            case StatementTypes.DATABASE_BACKUP : {
                String  path       = (String) parameters[0];
                boolean blocking   = ((Boolean) parameters[1]).booleanValue();
                boolean script     = ((Boolean) parameters[2]).booleanValue();
                boolean compressed = ((Boolean) parameters[3]).booleanValue();
                boolean files      = ((Boolean) parameters[4]).booleanValue();

                try {
                    session.checkAdmin();

                    if (session.database.getType() != DatabaseType.DB_FILE) {
                        throw Error.error(ErrorCode.DATABASE_IS_MEMORY_ONLY);
                    }

                    if (session.database.isFilesReadOnly()) {
                        throw Error.error(ErrorCode.DATABASE_IS_READONLY);
                    }

                    if (session.database.logger.isStoredFileAccess()) {
                        throw Error.error(ErrorCode.ACCESS_IS_DENIED);
                    }

                    session.database.logger.backup(path, script, blocking,
                                                   compressed, files);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.DATABASE_CHECKPOINT : {
                boolean defrag = ((Boolean) parameters[0]).booleanValue();

                try {
                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.checkpoint(session, defrag, true);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_BACKUP_INCREMENT : {
                try {
                    boolean mode = ((Boolean) parameters[0]).booleanValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setIncrementBackup(mode);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_CACHE_ROWS : {
                try {
                    int     value = ((Integer) parameters[0]).intValue();
                    boolean check = parameters[1] == null;

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (check) {
                        value =
                            session.database.getProperties()
                                .getPropertyWithinRange(HsqlDatabaseProperties
                                    .hsqldb_cache_rows, value);
                    }

                    session.database.logger.setCacheMaxRows(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_CACHE_SIZE : {
                try {
                    int     value = ((Integer) parameters[0]).intValue();
                    boolean check = parameters[1] == null;

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (check) {
                        value =
                            session.database.getProperties()
                                .getPropertyWithinRange(HsqlDatabaseProperties
                                    .hsqldb_cache_size, value);
                    }

                    session.database.logger.setCacheSize(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_CHECK : {
                try {
                    long value1 = ((Long) parameters[0]).longValue();
                    long value2 = ((Long) parameters[1]).longValue();

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (session.isProcessingScript()) {
                        session.database.logger.setFilesTimestamp(value1);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_LOBS_SCALE : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (session.isProcessingScript()) {
                        session.database.logger.setLobFileScaleNoCheck(value);
                    } else {
                        session.database.logger.setLobFileScale(value);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_LOBS_COMPRESSED : {
                try {
                    boolean mode = ((Boolean) parameters[0]).booleanValue();

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (session.isProcessingScript()) {
                        session.database.logger.setLobFileCompressedNoCheck(
                            mode);
                    } else {
                        session.database.logger.setLobFileCompressed(mode);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_SCALE : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (session.isProcessingScript()) {
                        session.database.logger.setDataFileScaleNoCheck(value);
                    } else {
                        session.database.logger.setDataFileScale(value);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_SPACE : {
                try {
                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (session.database.getType() == DatabaseType.DB_RES) {
                        return Result.updateZeroResult;
                    }

                    if (session.database.isFilesReadOnly()) {
                        return Result.updateZeroResult;
                    }

                    if (parameters[0] instanceof Boolean) {
                        boolean value =
                            ((Boolean) parameters[0]).booleanValue();

                        session.database.logger.setDataFileSpaces(value);
                    } else {
                        int value = ((Integer) parameters[0]).intValue();

                        session.database.logger.setDataFileSpaces(value);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_DEFRAG : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (!session.database.getProperties().validateProperty(
                            HsqlDatabaseProperties.hsqldb_defrag_limit,
                            value)) {
                        throw Error.error(ErrorCode.X_42556);
                    }

                    session.database.logger.setDefagLimit(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_EVENT_LOG : {
                try {
                    int     value = ((Integer) parameters[0]).intValue();
                    boolean isSql = ((Boolean) parameters[1]).booleanValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setEventLogLevel(value, isSql);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_NIO : {
                try {
                    session.checkAdmin();
                    session.checkDDLWrite();

                    Object v = parameters[0];

                    if (v instanceof Boolean) {
                        boolean value =
                            ((Boolean) parameters[0]).booleanValue();

                        session.database.logger.setNioDataFile(value);
                    } else {
                        int value = ((Integer) parameters[0]).intValue();

                        session.database.logger.setNioMaxSize(value);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_LOG : {
                try {
                    boolean value = ((Boolean) parameters[0]).booleanValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setLogData(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_LOG_SIZE : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setLogSize(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_TEMP_PATH : {
                try {
                    String value = (String) parameters[0];

                    session.checkAdmin();
                    session.checkDDLWrite();

                    // no action
                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_SCRIPT_FORMAT : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setScriptType(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_FILES_WRITE_DELAY : {
                try {
                    int value = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.logger.setWriteDelay(value);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_AUTHENTICATION : {
                try {
                    Routine routine = (Routine) parameters[0];

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.userManager.setExtAuthenticationFunction(
                        routine);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_PASSWORD_CHECK : {
                try {
                    Routine routine = (Routine) parameters[0];

                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.userManager.setPasswordCheckFunction(
                        routine);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_PASSWORD_DIGEST : {
                try {
                    String algo = (String) parameters[0];

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (!session.isProcessingScript()) {
                        return Result.updateZeroResult;
                    }

                    session.database.granteeManager.setDigestAlgo(algo);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_SQL_COLLATION : {
                try {
                    String name = (String) parameters[0];
                    boolean padSpaces =
                        ((Boolean) parameters[1]).booleanValue();

                    /** @todo 1.9.0 - ensure no data in character columns */
                    session.checkAdmin();
                    session.checkDDLWrite();
                    session.database.collation.setCollation(name, padSpaces);
                    session.database.schemaManager.setSchemaChangeTimestamp();

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_SQL_REFERENTIAL_INTEGRITY : {
                boolean mode = ((Boolean) parameters[0]).booleanValue();

                session.checkAdmin();
                session.checkDDLWrite();
                session.database.setReferentialIntegrity(mode);

                return Result.updateZeroResult;
            }
            case StatementTypes.SET_DATABASE_SQL : {
                String  property = (String) parameters[0];
                boolean mode     = ((Boolean) parameters[1]).booleanValue();
                int     value    = ((Number) parameters[2]).intValue();

                session.checkAdmin();
                session.checkDDLWrite();

                if (HsqlDatabaseProperties.sql_live_object.equals(property)) {
                    session.database.setLiveObject(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_names.equals(
                        property)) {
                    session.database.setStrictNames(mode);
                } else if (HsqlDatabaseProperties.sql_regular_names.equals(
                        property)) {
                    session.database.setRegularNames(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_size.equals(
                        property)) {
                    session.database.setStrictColumnSize(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_types.equals(
                        property)) {
                    session.database.setStrictTypes(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_refs.equals(
                        property)) {
                    session.database.setStrictReferences(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_tdcd.equals(
                        property)) {
                    session.database.setStrictTDCD(mode);
                } else if (HsqlDatabaseProperties.sql_enforce_tdcu.equals(
                        property)) {
                    session.database.setStrictTDCU(mode);
                } else if (HsqlDatabaseProperties.jdbc_translate_tti_types
                        .equals(property)) {
                    session.database.setTranslateTTI(mode);
                } else if (HsqlDatabaseProperties.sql_char_literal.equals(
                        property)) {
                    session.database.setCharacterLiteral(mode);
                } else if (HsqlDatabaseProperties.sql_concat_nulls.equals(
                        property)) {
                    session.database.setConcatNulls(mode);
                } else if (HsqlDatabaseProperties.sql_nulls_first.equals(
                        property)) {
                    session.database.setNullsFirst(mode);
                } else if (HsqlDatabaseProperties.sql_nulls_order.equals(
                        property)) {
                    session.database.setNullsOrder(mode);
                } else if (HsqlDatabaseProperties.sql_unique_nulls.equals(
                        property)) {
                    session.database.setUniqueNulls(mode);
                } else if (HsqlDatabaseProperties.sql_convert_trunc.equals(
                        property)) {
                    session.database.setConvertTrunc(mode);
                } else if (HsqlDatabaseProperties.sql_avg_scale.equals(
                        property)) {
                    session.database.setAvgScale(value);
                } else if (HsqlDatabaseProperties.sql_double_nan.equals(
                        property)) {
                    session.database.setDoubleNaN(mode);
                } else if (HsqlDatabaseProperties.sql_longvar_is_lob.equals(
                        property)) {
                    session.database.setLongVarIsLob(mode);
                } else if (HsqlDatabaseProperties.sql_ignore_case.equals(
                        property)) {
                    session.database.setIgnoreCase(mode);
                    session.setIgnoreCase(mode);
                } else if (HsqlDatabaseProperties.sql_syntax_db2.equals(
                        property)) {
                    session.database.setSyntaxDb2(mode);
                } else if (HsqlDatabaseProperties.sql_syntax_mss.equals(
                        property)) {
                    session.database.setSyntaxMss(mode);
                } else if (HsqlDatabaseProperties.sql_syntax_mys.equals(
                        property)) {
                    session.database.setSyntaxMys(mode);
                } else if (HsqlDatabaseProperties.sql_syntax_ora.equals(
                        property)) {
                    session.database.setSyntaxOra(mode);
                } else if (HsqlDatabaseProperties.sql_syntax_pgs.equals(
                        property)) {
                    session.database.setSyntaxPgs(mode);
                }

                return Result.updateZeroResult;
            }
            case StatementTypes.SET_DATABASE_DEFAULT_INITIAL_SCHEMA : {
                HsqlName schema = (HsqlName) parameters[0];

                session.checkAdmin();
                session.checkDDLWrite();

                //
                session.database.schemaManager.setDefaultSchemaHsqlName(
                    schema);
                session.database.schemaManager.setSchemaChangeTimestamp();

                //
                return Result.updateZeroResult;
            }
            case StatementTypes.SET_DATABASE_DEFAULT_TABLE_TYPE : {
                Integer type = (Integer) parameters[0];

                session.checkAdmin();
                session.checkDDLWrite();

                //
                session.database.schemaManager.setDefaultTableType(
                    type.intValue());

                //
                return Result.updateZeroResult;
            }
            case StatementTypes.SET_DATABASE_TRANSACTION_CONTROL : {
                try {
                    int mode = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.database.txManager.setTransactionControl(session,
                            mode);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_DEFAULT_ISOLATION_LEVEL : {
                try {
                    int mode = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();

                    session.database.defaultIsolationLevel = mode;

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_TRANSACTION_CONFLICT : {
                try {
                    boolean mode = ((Boolean) parameters[0]).booleanValue();

                    session.checkAdmin();

                    session.database.txConflictRollback = mode;

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_GC : {
                try {
                    int count = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();

                    JavaSystem.gcFrequency = count;

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_PROPERTY : {
                try {
                    String property = (String) parameters[0];
                    Object value    = parameters[1];

                    session.checkAdmin();
                    session.checkDDLWrite();

                    // command is a no-op from 1.9
                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_RESULT_MEMORY_ROWS : {
                int size = ((Integer) parameters[0]).intValue();

                session.checkAdmin();
                session.database.setResultMaxMemoryRows(size);

                return Result.updateZeroResult;
            }
            case StatementTypes.SET_DATABASE_TEXT_SOURCE : {
                try {
                    String         source = (String) parameters[0];
                    HsqlProperties props  = null;

                    session.checkAdmin();

                    if (source.length() > 0) {
                        props = HsqlProperties.delimitedArgPairsToProps(source,
                                "=", ";", null);

                        if (props.getErrorKeys().length > 0) {
                            throw Error.error(ErrorCode.TEXT_TABLE_SOURCE,
                                              props.getErrorKeys()[0]);
                        }

                        session.database.logger.setDefaultTextTableProperties(
                            source, props);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_DATABASE_UNIQUE_NAME : {
                try {
                    String name = (String) parameters[0];

                    session.checkAdmin();
                    session.database.setDatabaseName(name);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.DATABASE_SCRIPT : {
                ScriptWriterText dsw  = null;
                String           name = (String) parameters[0];

                try {
                    session.checkAdmin();

                    if (name == null) {
                        return session.database.getScript(false);
                    } else {
                        dsw = new ScriptWriterText(session.database, name,
                                                   true, true, true);

                        dsw.writeAll();
                        dsw.close();

                        return Result.updateZeroResult;
                    }
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.DATABASE_SHUTDOWN : {
                try {
                    int mode = ((Integer) parameters[0]).intValue();

                    session.checkAdmin();
                    session.database.close(mode);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.CHECK_INDEX : {
                try {
                    HsqlName name = (HsqlName) parameters[0];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_NEW_TABLESPACE : {
                try {
                    HsqlName name = (HsqlName) parameters[0];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    session.checkAdmin();
                    session.checkDDLWrite();

                    if (!session.database.logger.isFileDatabase()) {
                        return Result.updateZeroResult;
                    }

                    if (session.database.logger.getDataFileSpaces() == 0) {
                        throw Error.error(ErrorCode.ACCESS_IS_DENIED);
                    }

                    if (table.getSpaceID()
                            != DataSpaceManager.tableIdDefault) {
                        return Result.updateZeroResult;
                    }

                    DataFileCache cache = session.database.logger.getCache();

                    // memory database
                    if (cache == null) {
                        return Result.updateZeroResult;
                    }

                    DataSpaceManager dataSpace = cache.spaceManager;
                    int tableSpaceID = dataSpace.getNewTableSpaceID();

                    table.setSpaceID(tableSpaceID);

                    // if cache exists, a memory table can get a space id
                    // it can then be converted to cached
                    if (!table.isCached()) {
                        return Result.updateZeroResult;
                    }

                    TableSpaceManager tableSpace =
                        dataSpace.getTableSpace(tableSpaceID);
                    PersistentStore store = table.getRowStore(session);

                    store.setSpaceManager(tableSpace);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_SET_TABLESPACE : {
                try {
                    HsqlName name    = (HsqlName) parameters[0];
                    int      spaceid = ((Integer) parameters[1]).intValue();
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    if (!session.isProcessingScript()) {
                        return Result.updateZeroResult;
                    }

                    if (table.getTableType() != TableBase.CACHED_TABLE) {
                        return Result.updateZeroResult;
                    }

                    if (table.getSpaceID()
                            != DataSpaceManager.tableIdDefault) {
                        return Result.updateZeroResult;
                    }

                    table.setSpaceID(spaceid);

                    if (table.store == null) {
                        return Result.updateZeroResult;
                    }

                    DataFileCache cache = session.database.logger.getCache();

                    if (cache == null) {
                        return Result.updateZeroResult;
                    }

                    DataSpaceManager dataSpace = cache.spaceManager;
                    TableSpaceManager tableSpace =
                        dataSpace.getTableSpace(table.getSpaceID());

                    table.store.setSpaceManager(tableSpace);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_CLUSTERED : {
                try {
                    HsqlName name     = (HsqlName) parameters[0];
                    int[]    colIndex = (int[]) parameters[1];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    StatementSchema.checkSchemaUpdateAuthorisation(session,
                            table.getSchemaName());

                    if (!table.isCached() && !table.isText()) {
                        throw Error.error(ErrorCode.ACCESS_IS_DENIED);
                    }

                    Index index = table.getIndexForColumns(session, colIndex);

                    if (index != null) {
                        Index[] indexes = table.getIndexList();

                        for (int i = 0; i < indexes.length; i++) {
                            indexes[i].setClustered(false);
                        }

                        index.setClustered(true);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_INDEX : {
                try {
                    HsqlName name  = (HsqlName) parameters[0];
                    String   value = (String) parameters[1];
                    Integer  type  = (Integer) parameters[2];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    if (session.isProcessingScript()) {
                        table.setIndexRoots(session, value);
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_READONLY : {
                try {
                    HsqlName name = (HsqlName) parameters[0];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);
                    boolean mode = ((Boolean) parameters[1]).booleanValue();

                    StatementSchema.checkSchemaUpdateAuthorisation(session,
                            table.getSchemaName());
                    table.setDataReadOnly(mode);
                    session.database.schemaManager.setSchemaChangeTimestamp();

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_TABLE_SOURCE :
            case StatementTypes.SET_TABLE_SOURCE_HEADER : {
                try {
                    HsqlName name = (HsqlName) parameters[0];
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    StatementSchema.checkSchemaUpdateAuthorisation(session,
                            table.getSchemaName());

                    if (!table.isText()) {
                        Exception e = Error.error(ErrorCode.X_S0522);

                        return Result.newErrorResult(e, sql);
                    }

                    if (parameters[1] != null) {
                        boolean mode =
                            ((Boolean) parameters[1]).booleanValue();

                        if (mode) {
                            ((TextTable) table).connect(session);
                        } else {
                            ((TextTable) table).disconnect();
                        }

                        session.database.schemaManager
                            .setSchemaChangeTimestamp();

                        return Result.updateZeroResult;
                    }

                    String  source = (String) parameters[2];
                    boolean isDesc = ((Boolean) parameters[3]).booleanValue();
                    boolean isHeader =
                        ((Boolean) parameters[4]).booleanValue();

                    if (isHeader) {
                        ((TextTable) table).setHeader(source);
                    } else {
                        ((TextTable) table).setDataSource(session, source,
                                                          isDesc, false);
                    }

                    return Result.updateZeroResult;
                } catch (Throwable e) {
                    if (!(e instanceof HsqlException)) {
                        e = Error.error(ErrorCode.GENERAL_IO_ERROR,
                                        e.toString());
                    }

                    if (session.isProcessingLog()
                            || session.isProcessingScript()) {
                        session.addWarning((HsqlException) e);
                        session.database.logger.logWarningEvent(
                            "Problem processing SET TABLE SOURCE", e);

                        return Result.updateZeroResult;
                    } else {
                        return Result.newErrorResult(e, sql);
                    }
                }
            }
            case StatementTypes.SET_TABLE_TYPE : {
                try {
                    HsqlName name = (HsqlName) parameters[0];
                    int      type = ((Integer) parameters[1]).intValue();

                    //
                    Table table =
                        session.database.schemaManager.getUserTable(name.name,
                            name.schema.name);

                    if (table.getTableType() == type) {
                        return Result.updateZeroResult;
                    }

                    StatementSchema.checkSchemaUpdateAuthorisation(session,
                            table.getSchemaName());

                    TableWorks tw     = new TableWorks(session, table);
                    boolean    result = tw.setTableType(session, type);

                    if (!result) {
                        throw Error.error(ErrorCode.GENERAL_IO_ERROR);
                    }

                    session.database.schemaManager.setSchemaChangeTimestamp();

                    if (name.schema == SqlInvariants.LOBS_SCHEMA_HSQLNAME) {
                        session.database.lobManager.compileStatements();
                    }

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_USER_LOCAL : {
                User    user = (User) parameters[0];
                boolean mode = ((Boolean) parameters[1]).booleanValue();

                session.checkAdmin();
                session.checkDDLWrite();

                user.isLocalOnly = mode;

                session.database.schemaManager.setSchemaChangeTimestamp();

                return Result.updateZeroResult;
            }
            case StatementTypes.SET_USER_INITIAL_SCHEMA : {
                try {
                    User     user   = (User) parameters[0];
                    HsqlName schema = (HsqlName) parameters[1];

                    session.checkDDLWrite();

                    if (user == null) {
                        user = session.getUser();
                    } else {
                        session.checkAdmin();
                        session.checkDDLWrite();

                        user = session.database.userManager.get(
                            user.getName().getNameString());
                    }

                    if (schema != null) {
                        schema =
                            session.database.schemaManager.getSchemaHsqlName(
                                schema.name);
                    }

                    //
                    user.setInitialSchema(schema);
                    session.database.schemaManager.setSchemaChangeTimestamp();

                    //
                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.SET_USER_PASSWORD : {
                try {
                    User    user = parameters[0] == null ? session.getUser()
                                                         : (User) parameters[0];
                    String  password = (String) parameters[1];
                    boolean isDigest = (Boolean) parameters[2];

                    session.checkDDLWrite();
                    session.database.userManager.setPassword(session, user,
                            password, isDigest);

                    return Result.updateZeroResult;
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }
            }
            case StatementTypes.ALTER_SESSION : {
                try {
                    long sessionID = ((Number) parameters[0]).longValue();
                    int  action    = ((Number) parameters[1]).intValue();
                    Session targetSession =
                        session.database.sessionManager.getSession(sessionID);

                    if (targetSession == null) {
                        throw Error.error(ErrorCode.X_2E000);
                    }

                    switch (action) {

                        case Tokens.ALL :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionResetAll);
                            break;

                        case Tokens.TABLE :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionTables);
                            break;

                        case Tokens.RESULT :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionResults);
                            break;

                        case Tokens.CLOSE :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionClose);
                            break;

                        case Tokens.RELEASE :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionRollback);
                            break;

                        case Tokens.END :
                            session.database.txManager.resetSession(session,
                                    targetSession,
                                    TransactionManager.resetSessionAbort);
                            break;

                    }
                } catch (HsqlException e) {
                    return Result.newErrorResult(e, sql);
                }

                return Result.updateZeroResult;
            }
            default :
                throw Error.runtimeError(ErrorCode.U_S0500,
                                         "StatementCommand");
        }
    }

    Result getTruncateResult(Session session) {

        try {
            HsqlName name            = (HsqlName) parameters[0];
            boolean  restartIdentity = (Boolean) parameters[1];
            boolean  noCheck         = (Boolean) parameters[2];
            Table[]  tables;

            if (name.type == SchemaObject.TABLE) {
                Table table =
                    session.database.schemaManager.getUserTable(name);

                tables = new Table[]{ table };

                session.getGrantee().checkDelete(table);

                if (!noCheck) {
                    for (int i = 0; i < table.fkMainConstraints.length; i++) {
                        if (table.fkMainConstraints[i].getRef() != table) {
                            HsqlName tableName =
                                table.fkMainConstraints[i].getRef().getName();
                            Table refTable =
                                session.database.schemaManager.getUserTable(
                                    tableName);

                            if (!refTable.isEmpty(session)) {
                                throw Error.error(ErrorCode.X_23504,
                                                  refTable.getName().name);
                            }
                        }
                    }
                }
            } else {

                // ensure schema existence
                session.database.schemaManager.getSchemaHsqlName(name.name);

                HashMappedList list =
                    session.database.schemaManager.getTables(name.name);

                tables = new Table[list.size()];

                list.toValuesArray(tables);
                StatementSchema.checkSchemaUpdateAuthorisation(session, name);

                if (!noCheck) {
                    OrderedHashSet set = new OrderedHashSet();

                    session.database.schemaManager
                        .getCascadingReferencesToSchema(name, set);

                    for (int i = 0; i < set.size(); i++) {
                        HsqlName objectName = (HsqlName) set.get(i);

                        if (objectName.type == SchemaObject.CONSTRAINT) {
                            if (objectName.parent.type == SchemaObject.TABLE) {
                                Table refTable =
                                    session.database.schemaManager
                                        .getUserTable(objectName.parent);

                                if (!refTable.isEmpty(session)) {
                                    throw Error.error(ErrorCode.X_23504,
                                                      refTable.getName().name);
                                }
                            }
                        }
                    }
                }

                if (restartIdentity) {
                    Iterator it =
                        session.database.schemaManager.databaseObjectIterator(
                            name.name, SchemaObject.SEQUENCE);

                    while (it.hasNext()) {
                        NumberSequence sequence = (NumberSequence) it.next();

                        sequence.reset();
                    }
                }
            }

            for (int i = 0; i < tables.length; i++) {
                Table           table = tables[i];
                PersistentStore store = table.getRowStore(session);

                store.removeAll();

                if (restartIdentity && table.identitySequence != null) {
                    table.identitySequence.reset();
                }
            }

            return Result.updateZeroResult;
        } catch (HsqlException e) {
            return Result.newErrorResult(e, sql);
        }
    }

    public ResultMetaData getResultMetaData() {

        switch (type) {

            case StatementTypes.EXPLAIN_PLAN :
                return ResultMetaData.newSingleColumnMetaData("OPERATION");

            case StatementTypes.DATABASE_SCRIPT :
                if (statementReturnType == StatementTypes.RETURN_RESULT) {
                    return ResultMetaData.newSingleColumnMetaData("COMMANDS");
                }

            // fall through
            default :
                return super.getResultMetaData();
        }
    }

    public boolean isAutoCommitStatement() {
        return isTransactionStatement;
    }

    public String describe(Session session) {
        return sql;
    }
}