/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.aws.secretsmanager;

import java.util.Base64;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest.Builder;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;

/**
 * A Producer which sends messages to the Amazon Secrets Manager Service SDK v2
 * <a href="http://aws.amazon.com/secrets-manager/">AWS Secrets Manager</a>
 */
public class SecretsManagerProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(SecretsManagerProducer.class);

    private transient String secretsManagerProducerToString;

    public SecretsManagerProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        switch (determineOperation(exchange)) {
            case listSecrets:
                listSecrets(getEndpoint().getSecretsManagerClient(), exchange);
                break;
            case createSecret:
                createSecret(getEndpoint().getSecretsManagerClient(), exchange);
                break;
            case getSecret:
                getSecret(getEndpoint().getSecretsManagerClient(), exchange);
                break;
            case describeSecret:
                describeSecret(getEndpoint().getSecretsManagerClient(), exchange);
                break;
            case deleteSecret:
                deleteSecret(getEndpoint().getSecretsManagerClient(), exchange);
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation");
        }
    }

    private SecretsManagerOperations determineOperation(Exchange exchange) {
        SecretsManagerOperations operation = exchange.getIn().getHeader(SecretsManagerConstants.OPERATION,
                SecretsManagerOperations.class);
        if (operation == null) {
            operation = getConfiguration().getOperation();
        }
        return operation;
    }

    protected SecretsManagerConfiguration getConfiguration() {
        return getEndpoint().getConfiguration();
    }

    @Override
    public String toString() {
        if (secretsManagerProducerToString == null) {
            secretsManagerProducerToString = "SecretsManagerProducer["
                                             + URISupport.sanitizeUri(getEndpoint().getEndpointUri()) + "]";
        }
        return secretsManagerProducerToString;
    }

    @Override
    public SecretsManagerEndpoint getEndpoint() {
        return (SecretsManagerEndpoint) super.getEndpoint();
    }

    private void listSecrets(SecretsManagerClient secretsManagerClient, Exchange exchange)
            throws InvalidPayloadException {
        ListSecretsRequest request = null;
        ListSecretsResponse result;
        if (getConfiguration().isPojoRequest()) {
            Object payload = exchange.getIn().getMandatoryBody();
            if (payload instanceof ListSecretsRequest) {
                request = (ListSecretsRequest) payload;
            }
        } else {
            Builder builder = ListSecretsRequest.builder();
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.MAX_RESULTS))) {
                int maxRes = exchange.getIn().getHeader(SecretsManagerConstants.MAX_RESULTS, Integer.class);
                builder.maxResults(maxRes);
            }
            request = builder.build();
        }
        try {
            result = secretsManagerClient.listSecrets(request);
        } catch (AwsServiceException ase) {
            LOG.trace("List Secrets command returned the error code {}", ase.awsErrorDetails().errorCode());
            throw ase;
        }
        Message message = getMessageForResponse(exchange);
        message.setBody(result);
    }

    private void createSecret(SecretsManagerClient secretsManagerClient, Exchange exchange)
            throws InvalidPayloadException {
        CreateSecretRequest request = null;
        CreateSecretResponse result;
        if (getConfiguration().isPojoRequest()) {
            Object payload = exchange.getIn().getMandatoryBody();
            if (payload instanceof CreateSecretRequest) {
                request = (CreateSecretRequest) payload;
            }
        } else {
            CreateSecretRequest.Builder builder = CreateSecretRequest.builder();
            String payload = exchange.getIn().getMandatoryBody(String.class);
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.SECRET_NAME))) {
                String secretName = exchange.getIn().getHeader(SecretsManagerConstants.SECRET_NAME, String.class);
                builder.name(secretName);
            } else {
                throw new IllegalArgumentException("Secret Name must be specified");
            }
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.SECRET_DESCRIPTION))) {
                String descr = exchange.getIn().getHeader(SecretsManagerConstants.SECRET_DESCRIPTION, String.class);
                builder.description(descr);
            }
            if (getConfiguration().isBinaryPayload()) {
                builder.secretBinary(SdkBytes.fromUtf8String(Base64.getEncoder().encodeToString(payload.getBytes())));
            } else {
                builder.secretString((String) payload);
            }
            request = builder.build();
        }
        try {
            result = secretsManagerClient.createSecret(request);
        } catch (AwsServiceException ase) {
            LOG.trace("Create Secret command returned the error code {}", ase.awsErrorDetails().errorCode());
            throw ase;
        }
        Message message = getMessageForResponse(exchange);
        message.setBody(result);
    }

    private void getSecret(SecretsManagerClient secretsManagerClient, Exchange exchange)
            throws InvalidPayloadException {
        GetSecretValueRequest request = null;
        GetSecretValueResponse result;
        if (getConfiguration().isPojoRequest()) {
            Object payload = exchange.getIn().getMandatoryBody();
            if (payload instanceof GetSecretValueRequest) {
                request = (GetSecretValueRequest) payload;
            }
        } else {
            GetSecretValueRequest.Builder builder = GetSecretValueRequest.builder();
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID))) {
                String secretId = exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID, String.class);
                builder.secretId(secretId);
            } else {
                throw new IllegalArgumentException("Secret Id must be specified");
            }
            request = builder.build();
        }
        try {
            result = secretsManagerClient.getSecretValue(request);
        } catch (AwsServiceException ase) {
            LOG.trace("Get Secret value command returned the error code {}", ase.awsErrorDetails().errorCode());
            throw ase;
        }
        Message message = getMessageForResponse(exchange);
        if (getConfiguration().isBinaryPayload()) {
            message.setBody(new String(Base64.getDecoder().decode(result.secretBinary().asByteBuffer()).array()));
        } else {
            message.setBody(result.secretString());
        }
    }

    private void describeSecret(SecretsManagerClient secretsManagerClient, Exchange exchange)
            throws InvalidPayloadException {
        DescribeSecretRequest request = null;
        DescribeSecretResponse result;
        if (getConfiguration().isPojoRequest()) {
            Object payload = exchange.getIn().getMandatoryBody();
            if (payload instanceof DescribeSecretRequest) {
                request = (DescribeSecretRequest) payload;
            }
        } else {
            DescribeSecretRequest.Builder builder = DescribeSecretRequest.builder();
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID))) {
                String secretId = exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID, String.class);
                builder.secretId(secretId);
            } else {
                throw new IllegalArgumentException("Secret Id must be specified");
            }
            request = builder.build();
        }
        try {
            result = secretsManagerClient.describeSecret(request);
        } catch (AwsServiceException ase) {
            LOG.trace("Describe Secret value command returned the error code {}", ase.awsErrorDetails().errorCode());
            throw ase;
        }
        Message message = getMessageForResponse(exchange);
        message.setBody(result);
    }

    private void deleteSecret(SecretsManagerClient secretsManagerClient, Exchange exchange)
            throws InvalidPayloadException {
        DeleteSecretRequest request = null;
        DeleteSecretResponse result;
        if (getConfiguration().isPojoRequest()) {
            Object payload = exchange.getIn().getMandatoryBody();
            if (payload instanceof DeleteSecretRequest) {
                request = (DeleteSecretRequest) payload;
            }
        } else {
            DeleteSecretRequest.Builder builder = DeleteSecretRequest.builder();
            if (ObjectHelper.isNotEmpty(exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID))) {
                String secretId = exchange.getIn().getHeader(SecretsManagerConstants.SECRET_ID, String.class);
                builder.secretId(secretId);
            } else {
                throw new IllegalArgumentException("Secret Id must be specified");
            }
            request = builder.build();
        }
        try {
            result = secretsManagerClient.deleteSecret(request);
        } catch (AwsServiceException ase) {
            LOG.trace("Delete Secret value command returned the error code {}", ase.awsErrorDetails().errorCode());
            throw ase;
        }
        Message message = getMessageForResponse(exchange);
        message.setBody(result);
    }

    public static Message getMessageForResponse(final Exchange exchange) {
        return exchange.getMessage();
    }
}
