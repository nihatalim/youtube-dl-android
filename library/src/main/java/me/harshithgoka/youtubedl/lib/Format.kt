package me.harshithgoka.youtubedl.lib

import me.harshithgoka.youtubedl.lib.Utils.FormatUtils

/**
 * Created by harshithgoka on 12/16/2017 AD.
 */

class Format(var title: String) {

    var itag: Int = 0
        set(value) {
            field = value

            extension = FormatUtils.getExtension(this)
            content = FormatUtils.getTitle(this)
            description = FormatUtils.getDescription(this)
        }

    var url: String? = null
    var quality: String? = null
    var type: String? = null

    var extension: String? = null
    var content: String? = null
    var description: String? = null

    var audio: Boolean = false
    var video: Boolean = false

    var dowmloadState: DownloadState = DownloadState.NOT_DOWNLOADED
    var location: String? = null

    enum class DownloadState {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED
    }

    fun sanitizeFilename(): String {
        return title.replace("/".toRegex(), "|")
    }
}