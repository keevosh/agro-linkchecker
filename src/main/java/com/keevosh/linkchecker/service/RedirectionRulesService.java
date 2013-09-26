package com.keevosh.linkchecker.service;

import com.keevosh.linkchecker.dto.RedirectionRule;
import com.keevosh.linkchecker.dto.UrlDto;
import com.keevosh.linkchecker.options.LinkCheckerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public final class RedirectionRulesService {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectionRulesService.class);
    private static final Map<String, List<RedirectionRule>> RULES_MAP = new HashMap<String, List<RedirectionRule>>();

    private static final class HOLDER {
        public static final RedirectionRulesService SELF = new RedirectionRulesService();
    }

    private RedirectionRulesService() {}

    public static RedirectionRulesService getInstance() {
        return HOLDER.SELF;
    }

    public void initializeRules(LinkCheckerOptions options) {
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(new File(options.getRulesPath()));
        } catch (IOException e) {
            LOG.error("Error loading rules file from path {}. No redirection rules initialized.", options.getRulesPath(), e);
            return;
        }

        for (String line : lines) {
            try {
                RedirectionRule rule = new RedirectionRule(line.split(","));
                List<RedirectionRule> rulesList = RULES_MAP.get(rule.getDomain());
                if (rulesList == null) {
                    rulesList = new ArrayList<RedirectionRule>();
                }
                rulesList.add(rule);
                RULES_MAP.put(rule.getDomain(), rulesList);
            } catch (Exception e) {
                LOG.error("Error loading rule {}. Ignoring it.", line, e);
            }
        }
    }

    public Integer isRedirectionValid(UrlDto url) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("location", url);
        if (RULES_MAP.containsKey(url.getDomain())) {
            for (RedirectionRule rule : RULES_MAP.get(url.getDomain())) {
                if (rule.getCompiledExpression().getValue(context, Boolean.class)) {
                    LOG.info("Rule with id: {} matched for location {}", rule.getId(), url);
                    return rule.getResult();
                }
            }
        }
        return null;
    }
}
