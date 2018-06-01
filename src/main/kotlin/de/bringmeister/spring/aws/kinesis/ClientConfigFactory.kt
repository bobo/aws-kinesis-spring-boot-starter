package de.bringmeister.spring.aws.kinesis

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream.TRIM_HORIZON
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import java.net.InetAddress
import java.util.UUID

class ClientConfigFactory(
    private val credentialsProvider: AWSCredentialsProvider,
    private val awsCredentialsProviderFactory: AWSCredentialsProviderFactory,
    private val kinesisSettings: AwsKinesisSettings
) {

    fun consumerConfig(streamName: String): KinesisClientLibConfiguration {

        val consumerSettings = kinesisSettings.getConsumerSettingsOrDefault(streamName)
        val roleToAssume = "arn:aws:iam::${consumerSettings.awsAccountId}:role/${consumerSettings.iamRoleToAssume}"
        val credentials = awsCredentialsProviderFactory.credentials(roleToAssume)
        val workerId = InetAddress.getLocalHost().canonicalHostName + ":" + UUID.randomUUID()
        val applicationName = "${kinesisSettings.consumerGroup}_$streamName"

        return KinesisClientLibConfiguration(
            applicationName,
            streamName,
            credentials,
            credentialsProvider,
            credentialsProvider,
            workerId
        )
            .withInitialPositionInStream(TRIM_HORIZON)
            .withKinesisEndpoint(kinesisSettings.kinesisUrl)
            .withMetricsLevel(kinesisSettings.metricsLevel)
            .withDynamoDBEndpoint(kinesisSettings.dynamoDbSettings!!.url)
            .withInitialLeaseTableReadCapacity(kinesisSettings.dynamoDbSettings!!.leaseTableReadCapacity)
            .withInitialLeaseTableWriteCapacity(kinesisSettings.dynamoDbSettings!!.leaseTableWriteCapacity)
    }
}