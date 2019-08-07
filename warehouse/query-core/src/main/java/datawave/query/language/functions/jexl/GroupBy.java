package datawave.query.language.functions.jexl;

import datawave.query.jexl.functions.QueryFunctions;
import datawave.query.language.functions.QueryFunction;
import datawave.webservice.query.exception.BadRequestQueryException;
import datawave.webservice.query.exception.DatawaveErrorCode;

import java.text.MessageFormat;
import java.util.ArrayList;

public class GroupBy extends JexlQueryFunction {
    
    public GroupBy() {
        super(QueryFunctions.GROUPBY_FUNCTION, new ArrayList<>());
    }
    
    /**
     * query options is a list of fields. Cannot be the empty list.
     * 
     * @throws IllegalArgumentException
     */
    @Override
    public void validate() throws IllegalArgumentException {
        if (this.parameterList.isEmpty()) {
            BadRequestQueryException qe = new BadRequestQueryException(DatawaveErrorCode.INVALID_FUNCTION_ARGUMENTS, MessageFormat.format("{0}", this.name));
            throw new IllegalArgumentException(qe);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(QueryFunctions.QUERY_FUNCTION_NAMESPACE).append(':').append(QueryFunctions.GROUPBY_FUNCTION);
        if (parameterList.isEmpty()) {
            sb.append("()");
        } else {
            char separator = '(';
            for (String parm : parameterList) {
                sb.append(separator).append(escapeString(parm));
                separator = ',';
            }
            sb.append(')');
        }
        
        return sb.toString();
    }
    
    @Override
    public QueryFunction duplicate() {
        return new GroupBy();
    }
}
