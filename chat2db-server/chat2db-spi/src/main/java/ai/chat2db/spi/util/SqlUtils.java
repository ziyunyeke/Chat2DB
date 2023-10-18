
package ai.chat2db.spi.util;

import ai.chat2db.server.tools.base.excption.BusinessException;
import ai.chat2db.spi.enums.DataTypeEnum;
import ai.chat2db.spi.model.ExecuteResult;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jipengfei
 * @version : SqlUtils.java
 */
public class SqlUtils {

    public static final String DEFAULT_TABLE_NAME = "table1";

    public static void buildCanEditResult(String sql, DbType dbType, ExecuteResult executeResult) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                if (plainSelect.getJoins() == null && plainSelect.getFromItem() != null) {
                    for (SelectItem item : plainSelect.getSelectItems()) {
                        if (item instanceof SelectExpressionItem) {
                            SelectExpressionItem expressionItem = (SelectExpressionItem) item;
                            if (expressionItem.getAlias() != null) {
                                //canEdit = false; // 找到了一个别名
                                executeResult.setCanEdit(false);
                                return ;
                            }
                        }
                    }
                    executeResult.setCanEdit(true);
                    SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, dbType);
                    if ((sqlStatement instanceof SQLSelectStatement sqlSelectStatement)) {
                        SQLExprTableSource sqlExprTableSource = (SQLExprTableSource) getSQLExprTableSource(
                                sqlSelectStatement.getSelect().getFirstQueryBlock().getFrom());
                        executeResult.setTableName(getMetaDataTableName(sqlExprTableSource.getCatalog(), sqlExprTableSource.getSchema(), sqlExprTableSource.getTableName()));
                    }
                }
            }
        } catch (Exception e) {
            executeResult.setCanEdit(false);
        }
    }

    private static String getMetaDataTableName(String... names) {
        return Arrays.stream(names).filter(name -> StringUtils.isNotBlank(name)).map(name ->  name ).collect(Collectors.joining("."));
    }

    public static String formatSQLString(Object para) {
        return para != null ? " '" + para + "' " : null;
    }

    public static String getTableName(String sql, DbType dbType) {
        SQLStatement sqlStatement = SQLUtils.parseSingleStatement(sql, dbType);
        if (!(sqlStatement instanceof SQLSelectStatement sqlSelectStatement)) {
            throw new BusinessException("dataSource.sqlAnalysisError");
        }
        SQLExprTableSource sqlExprTableSource = (SQLExprTableSource) getSQLExprTableSource(
                sqlSelectStatement.getSelect().getFirstQueryBlock().getFrom());
        if (sqlExprTableSource == null) {
            return DEFAULT_TABLE_NAME;
        }
        return sqlExprTableSource.getTableName();
    }

    private static SQLTableSource getSQLExprTableSource(SQLTableSource sqlTableSource) {
        if (sqlTableSource instanceof SQLExprTableSource sqlExprTableSource) {
            return sqlExprTableSource;
        } else if (sqlTableSource instanceof SQLJoinTableSource sqlJoinTableSource) {
            return getSQLExprTableSource(sqlJoinTableSource.getLeft());
        }
        return null;
    }

    public static List<String> parse(String sql, DbType dbType) {
        List<String> list = new ArrayList<>();
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            // 遍历每个语句
            for (Statement stmt : statements.getStatements()) {
                list.add(stmt.toString());
            }
        } catch (Exception e) {
            list = SQLParserUtils.splitAndRemoveComment(sql, dbType);
        }
        return list;
    }

    public static String getSqlValue(String value, String dataType) {
        if (value == null) {
            return null;
        }
        DataTypeEnum dataTypeEnum = DataTypeEnum.getByCode(dataType);
        return dataTypeEnum.getSqlValue(value);
    }
}