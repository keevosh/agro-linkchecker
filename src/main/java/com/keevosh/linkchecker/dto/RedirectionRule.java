package com.keevosh.linkchecker.dto;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class RedirectionRule {
    private static final transient ExpressionParser PARSER = new SpelExpressionParser();
    private String id;
    private String domain;
    private int code;
    private Expression compiledExpression;
    private int result;

    /**
     * 
     */
    public RedirectionRule(String... rules) {
        super();
        int i = 0;
        this.id = rules[i++].trim();
        this.domain = rules[i++].trim();
        this.code = Integer.valueOf(rules[i++].trim());
        this.compiledExpression = PARSER.parseExpression(rules[i++].trim());
        this.result = Integer.valueOf(rules[i++].trim());
    }

    /**
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * @param code
     *            the code to set
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return the result
     */
    public int getResult() {
        return result;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param domain
     *            the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @param result
     *            the result to set
     */
    public void setResult(int result) {
        this.result = result;
    }

    /**
     * @return the compiledExpression
     */
    public Expression getCompiledExpression() {
        return compiledExpression;
    }

    /**
     * @param compiledExpression
     *            the compiledExpression to set
     */
    public void setCompiledExpression(Expression compiledExpression) {
        this.compiledExpression = compiledExpression;
    }

}