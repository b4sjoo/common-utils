/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.commons.notifications.model

import org.opensearch.common.Strings
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.io.stream.Writeable
import org.opensearch.common.xcontent.XContentParserUtils
import org.opensearch.commons.notifications.NotificationConstants.CONFIG_ID_TAG
import org.opensearch.commons.notifications.NotificationConstants.CONFIG_TYPE_TAG
import org.opensearch.commons.notifications.NotificationConstants.DESCRIPTION_TAG
import org.opensearch.commons.notifications.NotificationConstants.IS_ENABLED_TAG
import org.opensearch.commons.notifications.NotificationConstants.NAME_TAG
import org.opensearch.commons.utils.logger
import org.opensearch.core.xcontent.ToXContent
import org.opensearch.core.xcontent.XContentBuilder
import org.opensearch.core.xcontent.XContentParser
import java.io.IOException

/**
 * Data class representing Notification config exposed for other plugins.
 */
data class Channel(
    val configId: String,
    val name: String,
    val description: String,
    val configType: ConfigType,
    val isEnabled: Boolean = true
) : BaseModel {

    init {
        require(!Strings.isNullOrEmpty(name)) { "name is null or empty" }
        require(!Strings.isNullOrEmpty(configId)) { "config id is null or empty" }
    }

    companion object {
        private val log by logger(Channel::class.java)

        /**
         * reader to create instance of class from writable.
         */
        val reader = Writeable.Reader { Channel(it) }

        /**
         * Creator used in REST communication.
         * @param parser XContentParser to deserialize data from.
         */
        @Suppress("ComplexMethod")
        @JvmStatic
        @Throws(IOException::class)
        fun parse(parser: XContentParser): Channel {
            var configId: String? = null
            var name: String? = null
            var description = ""
            var configType: ConfigType? = null
            var isEnabled = true

            XContentParserUtils.ensureExpectedToken(
                XContentParser.Token.START_OBJECT,
                parser.currentToken(),
                parser
            )
            while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                val fieldName = parser.currentName()
                parser.nextToken()
                when (fieldName) {
                    CONFIG_ID_TAG -> configId = parser.text()
                    NAME_TAG -> name = parser.text()
                    DESCRIPTION_TAG -> description = parser.text()
                    CONFIG_TYPE_TAG -> configType = ConfigType.fromTagOrDefault(parser.text())
                    IS_ENABLED_TAG -> isEnabled = parser.booleanValue()
                    else -> {
                        parser.skipChildren()
                        log.info("Unexpected field: $fieldName, while parsing Channel")
                    }
                }
            }
            configId ?: throw IllegalArgumentException("$CONFIG_ID_TAG field absent")
            name ?: throw IllegalArgumentException("$NAME_TAG field absent")
            configType ?: throw IllegalArgumentException("$CONFIG_TYPE_TAG field absent")
            return Channel(
                configId,
                name,
                description,
                configType,
                isEnabled
            )
        }
    }

    /**
     * Constructor used in transport action communication.
     * @param input StreamInput stream to deserialize data from.
     */
    constructor(input: StreamInput) : this(
        configId = input.readString(),
        name = input.readString(),
        description = input.readString(),
        configType = input.readEnum(ConfigType::class.java),
        isEnabled = input.readBoolean()
    )

    /**
     * {@inheritDoc}
     */
    override fun writeTo(output: StreamOutput) {
        output.writeString(configId)
        output.writeString(name)
        output.writeString(description)
        output.writeEnum(configType)
        output.writeBoolean(isEnabled)
    }

    /**
     * {@inheritDoc}
     */
    override fun toXContent(builder: XContentBuilder?, params: ToXContent.Params?): XContentBuilder {
        builder!!
        return builder.startObject()
            .field(CONFIG_ID_TAG, configId)
            .field(NAME_TAG, name)
            .field(DESCRIPTION_TAG, description)
            .field(CONFIG_TYPE_TAG, configType.tag)
            .field(IS_ENABLED_TAG, isEnabled)
            .endObject()
    }
}
