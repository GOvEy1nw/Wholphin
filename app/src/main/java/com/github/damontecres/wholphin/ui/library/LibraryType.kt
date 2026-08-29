package com.github.damontecres.wholphin.ui.library

import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType

internal fun isVideoLibrary(
    type: BaseItemKind,
    collectionType: CollectionType,
    isRecordingFolder: Boolean,
): Boolean =
    !isRecordingFolder &&
        when (collectionType) {
            CollectionType.MOVIES,
            CollectionType.TVSHOWS,
            CollectionType.HOMEVIDEOS,
            -> true

            CollectionType.MUSICVIDEOS,
            CollectionType.FOLDERS,
            CollectionType.UNKNOWN,
            -> type == BaseItemKind.COLLECTION_FOLDER || type == BaseItemKind.USER_VIEW

            else -> false
        }
