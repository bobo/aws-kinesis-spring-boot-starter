package de.bringmeister.spring.aws.kinesis

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.DescribeStreamResult
import com.amazonaws.services.kinesis.model.ResourceNotFoundException
import com.amazonaws.services.kinesis.model.StreamDescription
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.doReturn
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class StreamInitializerTest {

    private var kinesis: AmazonKinesis = mock { }
    private var settings: AwsKinesisSettings = mock { }
    private var streamInitializer: StreamInitializer = StreamInitializer(kinesis, settings)

    @Test
    fun `should do nothing if stream already exists`() {
        doReturn(aDescriptionOfAnActiveStream())
            .whenever(kinesis)
            .describeStream("MY_STREAM")

        doReturn(true)
            .whenever(settings)
            .createStreams

        streamInitializer.createStreamIfMissing("MY_STREAM")
    }

    @Test
    fun `should create missing stream`() {

        doReturn(true)
            .whenever(settings)
            .createStreams

        doReturn(TimeUnit.SECONDS.toMillis(30))
            .whenever(settings)
            .creationTimeoutInMilliSeconds

        whenever(kinesis.describeStream("MY_STREAM"))
            .doThrow(ResourceNotFoundException("Stream not found!")) // not found exception
            .thenReturn(aDescriptionOfAStreamInCreation()) // in creation
            .thenReturn(aDescriptionOfAnActiveStream()) // finally active

        streamInitializer.createStreamIfMissing("MY_STREAM")

        verify(kinesis).createStream("MY_STREAM", 1)
    }

    @Test(expected = IllegalStateException::class)
    fun `should wait for stream in creation and timeout`() {

        doReturn(true)
            .whenever(settings)
            .createStreams

        doReturn(1L)
            .whenever(settings)
            .creationTimeoutInMilliSeconds

        doReturn(aDescriptionOfAStreamInCreation())
            .whenever(kinesis)
            .describeStream("MY_STREAM")

        streamInitializer.createStreamIfMissing("MY_STREAM")
    }

    private fun aDescriptionOfAnActiveStream(): DescribeStreamResult {
        return DescribeStreamResult()
            .withStreamDescription(
                StreamDescription()
                    .withStreamStatus("ACTIVE")
            )
    }

    private fun aDescriptionOfAStreamInCreation(): DescribeStreamResult {
        return DescribeStreamResult()
            .withStreamDescription(
                StreamDescription()
                    .withStreamStatus("CREATING")
            )
    }
}