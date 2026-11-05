package io.github.supermonster003.autojs6.plugin.three.chime.alarm.core.alarm

import org.autojs.plugin.alarm.api.AlarmErrorCodes

/**
 * A failure the plugin reports to its callers with one of the contract error codes (roadmap appendix A section 11).
 * The Binder service (roadmap P2.5) answers it as `errorCode` + `errorMessage`; the message is the encoded form
 * (`CODE: detail`) so it also survives being thrown across Binder.
 *
 * zh-CN: 插件以契约错误码向调用方报告的失败 (路线图附录 A 第 11 节). Binder 服务 (路线图 P2.5) 将其应答为 `errorCode` + `errorMessage`;
 * 消息为编码形式 (`CODE: detail`), 因此跨 Binder 抛出时也能保留.
 */
class AlarmException(val code: String, val detail: String = "") : RuntimeException(AlarmErrorCodes.encode(code, detail)) {

    companion object {

        fun invalidOptions(detail: String) = AlarmException(AlarmErrorCodes.INVALID_OPTIONS, detail)

        fun invalidRule(detail: String) = AlarmException(AlarmErrorCodes.INVALID_RULE, detail)

        fun notFound(detail: String) = AlarmException(AlarmErrorCodes.NOT_FOUND, detail)

        fun limitExceeded(detail: String) = AlarmException(AlarmErrorCodes.LIMIT_EXCEEDED, detail)

        fun unsupported(detail: String) = AlarmException(AlarmErrorCodes.UNSUPPORTED, detail)

        fun ioFailed(detail: String) = AlarmException(AlarmErrorCodes.IO_FAILED, detail)

        fun permissionRequired(detail: String) = AlarmException(AlarmErrorCodes.PERMISSION_REQUIRED, detail)

        /**
         * The contract parsers (`AlarmSpec.fromValues` and the snapshots) throw [IllegalArgumentException] whose message
         * is an encoded code; anything else becomes `INVALID_OPTIONS` with the message as detail.
         * zh-CN: 契约解析函数抛出消息为编码错误码的 [IllegalArgumentException]; 其他消息一律转为 `INVALID_OPTIONS`, 消息作为说明.
         */
        fun of(error: IllegalArgumentException): AlarmException {
            val decoded = AlarmErrorCodes.decode(error.message)
            return if (decoded != null) AlarmException(decoded.first, decoded.second)
            else invalidOptions(error.message ?: "invalid options")
        }
    }
}
