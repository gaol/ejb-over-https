/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejb.remote.client;

import org.jboss.as.quickstarts.ejb.remote.EchoService;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.URIAffinity;
import org.wildfly.httpclient.common.WildflyHttpContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;

/**
 * @author Lin Gao
 */
public class Main {

    private static final String HTTP_ENDPOINT = "http://localhost:8080/wildfly-services";

    private static final String HTTPS_ENDPOINT = "https://localhost:8443/wildfly-services";
    public static void main(String[] args) throws Exception {
        testEchoMessageOverHTTPS();
    }

    /**
     * Invoke remote EJB over HTTPS in a loop.
     *
     * @throws NamingException
     * @throws URISyntaxException
     */
    private static void testEchoMessageOverHTTPS() throws NamingException, URISyntaxException {
        for (int i = 0; i < Integer.getInteger("loops.size", 10); ++i) {
            System.out.println("\nExecuting " + (i + 1) + " times....\n");

            final String endpointURL = Boolean.getBoolean("ssl") ? HTTPS_ENDPOINT : HTTP_ENDPOINT;
            System.out.println("EndPoint URL: " + endpointURL);
            System.out.println("Clear Session Id...");
            URI uri = new URI(endpointURL);
            WildflyHttpContext.getCurrent().getTargetContext(uri).clearSessionId();
            final EchoService echoService = lookupEchoService(endpointURL);
            System.out.println("Set up Strong Affinity to the remote endpoint.");
            EJBClient.setStrongAffinity(echoService, URIAffinity.forUri(uri));

            String messageSent = "Hello World!";
            String message = echoService.echo(messageSent);
            if (!message.equals(messageSent)) {
                throw new RuntimeException("Message echo back is not equal.");
            }
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 10000; ++k) {
                sb.append("Hello World ");
            }
            String largeMessage = sb.toString();
            String lm = echoService.echo(largeMessage);
            if (!lm.equals(largeMessage)) {
                throw new RuntimeException("Large Message is not equal.");
            }
            System.out.println("Large message count: " + lm.length());
        }
    }

    private static EchoService lookupEchoService(String endpointURL) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProperties.put(Context.PROVIDER_URL, endpointURL);
        final Context context = new InitialContext(jndiProperties);

        return (EchoService) context.lookup("ejb:/ejb-over-https-server-side/EchoServiceBean!"
            + EchoService.class.getName());
    }

}
