package com.github.damontecres.wholphin.ui.cards

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.data.model.partitionCastAndCrew
import com.github.damontecres.wholphin.data.model.stringRes
import com.github.damontecres.wholphin.ui.handleDPadKeyEvents
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.tryRequestFocus

@Composable
fun PersonRow(
    people: List<Person>,
    showImages: Boolean,
    onClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    @StringRes title: Int = R.string.people_title,
    onLongClick: ((Int, Person) -> Unit)? = null,
) {
    if (showImages) {
        ImagePersonRow(
            people = people,
            onClick = onClick,
            modifier = modifier,
            focusRequester = focusRequester,
            title = title,
            onLongClick = onLongClick,
        )
    } else {
        PeopleSection(
            people = people,
            onClick = onClick,
            modifier = modifier,
            focusRequester = focusRequester,
        )
    }
}

@Composable
private fun ImagePersonRow(
    people: List<Person>,
    onClick: (Person) -> Unit,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    @StringRes title: Int,
    onLongClick: ((Int, Person) -> Unit)?,
) {
    val firstFocus = remember { FocusRequester() }
    var position by rememberInt()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            if (focusRequester == null) {
                modifier
            } else {
                modifier.focusRequester(focusRequester)
            },
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(8.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRestorer(firstFocus),
        ) {
            itemsIndexed(people) { index, person ->
                PersonCard(
                    person = person,
                    onClick = {
                        position = index
                        onClick.invoke(person)
                    },
                    onLongClick = {
                        position = index
                        onLongClick?.invoke(index, person)
                    },
                    modifier =
                        Modifier
                            .width(personRowCardWidth)
                            .ifElse(index == position, Modifier.focusRequester(firstFocus))
                            .animateItem(),
                )
            }
        }
    }
}

@Composable
private fun PeopleSection(
    people: List<Person>,
    onClick: (Person) -> Unit,
    modifier: Modifier,
    focusRequester: FocusRequester?,
) {
    val (cast, crew) = remember(people) { people.partitionCastAndCrew() }
    val castFocusRequesters =
        remember(cast, focusRequester) {
            List(cast.size) { index ->
                if (index == 0 && focusRequester != null) focusRequester else FocusRequester()
            }
        }
    val crewFocusRequesters =
        remember(cast, crew, focusRequester) {
            List(crew.size) { index ->
                if (cast.isEmpty() && index == 0 && focusRequester != null) focusRequester else FocusRequester()
            }
        }
    val castSplit = (cast.size + 1) / 2
    val crewSplit = (crew.size + 1) / 2
    val castColumns = listOf(cast.subList(0, castSplit), cast.subList(castSplit, cast.size))
    val crewColumns = listOf(crew.subList(0, crewSplit), crew.subList(crewSplit, crew.size))
    val castRequesterColumns =
        listOf(castFocusRequesters.subList(0, castSplit), castFocusRequesters.subList(castSplit, cast.size))
    val crewRequesterColumns =
        listOf(crewFocusRequesters.subList(0, crewSplit), crewFocusRequesters.subList(crewSplit, crew.size))
    val castRightEdge = castRequesterColumns[1].ifEmpty { castRequesterColumns[0] }
    val crewLeftEdge = crewRequesterColumns[0].ifEmpty { crewRequesterColumns[1] }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PeopleGroup(
                title = R.string.cast,
                peopleColumns = castColumns,
                focusRequesterColumns = castRequesterColumns,
                leftOutside = emptyList(),
                rightOutside = crewLeftEdge,
                onClick = onClick,
                modifier = Modifier.weight(1f),
            )
            PeopleGroup(
                title = R.string.crew,
                peopleColumns = crewColumns,
                focusRequesterColumns = crewRequesterColumns,
                leftOutside = castRightEdge,
                rightOutside = emptyList(),
                onClick = onClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PeopleGroup(
    @StringRes title: Int,
    peopleColumns: List<List<Person>>,
    focusRequesterColumns: List<List<FocusRequester>>,
    leftOutside: List<FocusRequester>,
    rightOutside: List<FocusRequester>,
    onClick: (Person) -> Unit,
    modifier: Modifier,
) {
    val columns = peopleColumns.zip(focusRequesterColumns)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            columns.forEachIndexed { columnIndex, (people, focusRequesters) ->
                val leftFocusRequesters =
                    columns
                        .take(columnIndex)
                        .asReversed()
                        .firstOrNull { (_, requesters) -> requesters.isNotEmpty() }
                        ?.second
                        ?: leftOutside
                val rightFocusRequesters =
                    columns
                        .drop(columnIndex + 1)
                        .firstOrNull { (_, requesters) -> requesters.isNotEmpty() }
                        ?.second
                        ?: rightOutside
                PeopleColumn(
                    people = people,
                    focusRequesters = focusRequesters,
                    leftFocusRequesters = leftFocusRequesters,
                    rightFocusRequesters = rightFocusRequesters,
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PeopleColumn(
    people: List<Person>,
    focusRequesters: List<FocusRequester>,
    leftFocusRequesters: List<FocusRequester>,
    rightFocusRequesters: List<FocusRequester>,
    onClick: (Person) -> Unit,
    modifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        people.forEachIndexed { index, person ->
            Surface(
                onClick = { onClick.invoke(person) },
                colors =
                    ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[index])
                        .handleDPadKeyEvents(
                            onLeft =
                                if (leftFocusRequesters.isNotEmpty()) {
                                    {
                                        leftFocusRequesters[index.coerceAtMost(leftFocusRequesters.lastIndex)]
                                            .tryRequestFocus()
                                    }
                                } else {
                                    null
                                },
                            onRight =
                                if (rightFocusRequesters.isNotEmpty()) {
                                    {
                                        rightFocusRequesters[index.coerceAtMost(rightFocusRequesters.lastIndex)]
                                            .tryRequestFocus()
                                    }
                                } else {
                                    null
                                },
                            onUp =
                                if (index > 0) {
                                    { focusRequesters[index - 1].tryRequestFocus() }
                                } else {
                                    null
                                },
                            onDown =
                                if (index < people.lastIndex) {
                                    { focusRequesters[index + 1].tryRequestFocus() }
                                } else {
                                    null
                                },
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Column {
                    Text(
                        text = person.name ?: stringResource(R.string.unknown),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                            person.role?.takeIf { it.isNotBlank() }
                                ?: person.type.stringRes?.let { stringResource(it) }
                                ?: stringResource(R.string.unknown),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverPersonRow(
    people: List<DiscoverItem>,
    onClick: (DiscoverItem) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int = R.string.people_title,
    onLongClick: ((Int, DiscoverItem) -> Unit)? = null,
) {
    val firstFocus = remember { FocusRequester() }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(8.dp),
            modifier =
                Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
                    .focusRestorer(firstFocus),
        ) {
            itemsIndexed(people) { index, person ->
                PersonCard(
                    name = person.title,
                    role = person.subtitle,
                    imageUrl = person.posterUrl,
                    favorite = false,
                    onClick = { onClick.invoke(person) },
                    onLongClick = { onLongClick?.invoke(index, person) },
                    modifier =
                        Modifier
                            .width(personRowCardWidth)
                            .ifElse(index == 0, Modifier.focusRequester(firstFocus))
                            .animateItem(),
                )
            }
        }
    }
}

val personRowCardWidth = 108.dp
