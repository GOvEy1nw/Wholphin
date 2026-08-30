package com.github.damontecres.wholphin.data.model

import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.PersonKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonTest {
    @Test
    fun partitionCastAndCrewKeepsPerformerKindsAndServerOrder() {
        val actor = person(PersonKind.ACTOR)
        val guestStar = person(PersonKind.GUEST_STAR)
        val artist = person(PersonKind.ARTIST)
        val albumArtist = person(PersonKind.ALBUM_ARTIST)
        val director = person(PersonKind.DIRECTOR)

        val (cast, crew) = listOf(director, actor, guestStar, artist, albumArtist).partitionCastAndCrew()

        assertEquals(listOf(actor, guestStar, artist, albumArtist), cast)
        assertEquals(listOf(director), crew)
    }

    private fun person(type: PersonKind) =
        Person(
            id = UUID.randomUUID(),
            name = type.name,
            role = null,
            type = type,
            imageUrl = null,
            favorite = false,
        )
}
