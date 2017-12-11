/*
 * Copyright © 2017 ForgeRock, AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 *
 * otpSelectorNode: Created by Charan Mann on 12/7/17 , 11:48 AM.
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.util.i18n.PreferredLocales;

import javax.inject.Inject;
import javax.security.auth.callback.ChoiceCallback;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.auth.node.api.Action.send;


/**
 * An authentication decision node which prompts with a choice to select OTP mode like Email, SMS etc
 */
@Node.Metadata(outcomeProvider = OTPSelectorNode.ChoiceCollectorOutcomeProvider.class,
        configClass = OTPSelectorNode.Config.class)
public class OTPSelectorNode implements Node {

    private final static String DEBUG_FILE = "OTPSelectorNode";
    private final Config config;
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private static final String BUNDLE = OTPSelectorNode.class.getName().replace(".", "/");

    /**
     * Guice constructor.
     *
     * @param config The service config for the node.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public OTPSelectorNode(@Assisted Config config)
            throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        if (context.hasCallbacks()) {
            ChoiceCallback choiceCallback = (ChoiceCallback) context.getAllCallbacks().get(0);
            return goTo(choiceCallback.getChoices()[choiceCallback.getSelectedIndexes()[0]]).build();
        } else {
            String[] choicesArr = new String[config.choices().size()];
            config.choices().toArray(choicesArr);

            PreferredLocales locales = context.request.locales;
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

            ChoiceCallback choiceCallback = new ChoiceCallback(bundle.getString("choices.select"), choicesArr, 0, false);
            return send(choiceCallback).build();
        }
    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100)
        List<String> choices();
    }

    /**
     * Provides the outcomes for the choice collector node.
     */
    public static class ChoiceCollectorOutcomeProvider implements OutcomeProvider {


        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            try {
                return nodeAttributes.get("choices").required()
                        .asList(String.class)
                        .stream()
                        .map(choice -> new Outcome(choice, (bundle.containsKey(choice)) ? bundle.getString(choice) : choice))
                        .collect(Collectors.toList());
            } catch (JsonValueException e) {
                return emptyList();
            }
        }
    }

}

