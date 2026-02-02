package ch.opum.tricktrack.ui.place

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.opum.tricktrack.R
import ch.opum.tricktrack.TripApplication
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.ui.ViewModelFactory
import kotlinx.coroutines.launch

// Data class for the generic list
data class SimpleItem(val id: Int, val title: String, val subtitle: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesListScreen(
    onAddPlace: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as TripApplication
    val viewModel: FavouritesViewModel = viewModel(
        factory = ViewModelFactory(
            application,
            application.repository,
            application.userPreferencesRepository
        )
    )

    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val tabTitles = listOf(
        stringResource(R.string.favourites_tab_places),
        stringResource(R.string.favourites_tab_drivers),
        stringResource(R.string.favourites_tab_companies),
        stringResource(R.string.favourites_tab_vehicles)
    )

    val addDriverTitle = stringResource(R.string.favourites_add_driver_title)
    val addCompanyTitle = stringResource(R.string.favourites_add_company_title)
    val addVehicleTitle = stringResource(R.string.favourites_add_vehicle_title)

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<SimpleItem?>(null) }
    var placeToEdit by remember { mutableStateOf<SavedPlace?>(null) }
    var dialogTitle by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                dialogTitle = when (selectedTabIndex) {
                    0 -> { onAddPlace(); "" }
                    1 -> addDriverTitle
                    2 -> addCompanyTitle
                    3 -> addVehicleTitle
                    else -> ""
                }
                if (selectedTabIndex != 0) {
                    showAddDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.favourites_add_item))
            }
        }

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(title) }
                )
            }
        }

        if (showAddDialog) {
            AddSimpleItemDialog(
                title = dialogTitle,
                onDismiss = { showAddDialog = false },
                onAdd = { name, subtitle ->
                    when (selectedTabIndex) {
                        1 -> viewModel.addDriver(name)
                        2 -> viewModel.addCompany(name)
                        3 -> viewModel.addVehicle(name, subtitle)
                    }
                    showAddDialog = false
                },
                isVehicle = selectedTabIndex == 3
            )
        }

        if (showEditDialog && itemToEdit != null) {
            EditSimpleItemDialog(
                item = itemToEdit!!,
                title = stringResource(R.string.favourites_edit_item),
                onDismiss = {
                    showEditDialog = false
                    itemToEdit = null
                },
                onSave = { updatedItem ->
                    when (selectedTabIndex) {
                        1 -> viewModel.updateDriver(viewModel.driversList.value.first{it.id == updatedItem.id}.copy(name = updatedItem.title))
                        2 -> viewModel.updateCompany(viewModel.companiesList.value.first{it.id == updatedItem.id}.copy(name = updatedItem.title))
                        3 -> viewModel.updateVehicle(viewModel.vehiclesList.value.first{it.id == updatedItem.id}.copy(licensePlate = updatedItem.title, carModel = updatedItem.subtitle))
                    }
                    showEditDialog = false
                    itemToEdit = null
                },
                onDelete = {
                     when (selectedTabIndex) {
                        1 -> viewModel.deleteDriver(viewModel.driversList.value.first{it.id == itemToEdit!!.id})
                        2 -> viewModel.deleteCompany(viewModel.companiesList.value.first{it.id == itemToEdit!!.id})
                        3 -> viewModel.deleteVehicle(viewModel.vehiclesList.value.first{it.id == itemToEdit!!.id})
                    }
                    showEditDialog = false
                    itemToEdit = null
                },
                isVehicle = selectedTabIndex == 3
            )
        }

        if (showEditDialog && placeToEdit != null) {
            AddEditPlaceDialog(
                place = placeToEdit,
                onDismiss = {
                    showEditDialog = false
                    placeToEdit = null
                },
                onSave = { name, address, lat, lon ->
                    viewModel.updatePlace(placeToEdit!!, name, address, lat, lon)
                    showEditDialog = false
                    placeToEdit = null
                },
                onDelete = {
                    viewModel.deletePlace(placeToEdit!!)
                    showEditDialog = false
                    placeToEdit = null
                },
                favouritesViewModel = viewModel
            )
        }


        when (selectedTabIndex) {
            0 -> {
                val groupedPlaces by viewModel.groupedPlaces.collectAsState()
                SavedPlaceList(
                    groupedFavorites = groupedPlaces,
                    onEditPlace = {
                        placeToEdit = it
                        showEditDialog = true
                    }
                )
            }
            1 -> {
                val groupedDrivers by viewModel.groupedDrivers.collectAsState()
                val selectedId by viewModel.selectedDriverId.collectAsState()
                GenericGroupedList(
                    groupedItems = groupedDrivers.mapValues { entry -> entry.value.map { SimpleItem(it.id, it.name) } },
                    onEdit = { item ->
                        itemToEdit = item
                        showEditDialog = true
                    },
                    selectedId = selectedId,
                    onSelect = { id -> viewModel.setDefaultDriver(id) }
                )
            }
            2 -> {
                val groupedCompanies by viewModel.groupedCompanies.collectAsState()
                val selectedId by viewModel.selectedCompanyId.collectAsState()
                GenericGroupedList(
                    groupedItems = groupedCompanies.mapValues { entry -> entry.value.map { SimpleItem(it.id, it.name) } },
                    onEdit = { item ->
                        itemToEdit = item
                        showEditDialog = true
                    },
                    selectedId = selectedId,
                    onSelect = { id -> viewModel.setDefaultCompany(id) }
                )
            }
            3 -> {
                val groupedVehicles by viewModel.groupedVehicles.collectAsState()
                val selectedId by viewModel.selectedVehicleId.collectAsState()
                GenericGroupedList(
                    groupedItems = groupedVehicles.mapValues { entry -> entry.value.map { SimpleItem(it.id, it.licensePlate, it.carModel) } },
                    onEdit = { item ->
                        itemToEdit = item
                        showEditDialog = true
                    },
                    selectedId = selectedId,
                    onSelect = { id -> viewModel.setDefaultVehicle(id) }
                )
            }
        }
    }
}

@Composable
fun AddSimpleItemDialog(title: String, onDismiss: () -> Unit, onAdd: (String, String?) -> Unit, isVehicle: Boolean) {
    var text by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isVehicle) stringResource(R.string.favourites_license_plate_label) else stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isVehicle) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text(stringResource(R.string.favourites_car_model_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAdd(text, if (isVehicle) subtitle else null) }) { Text(stringResource(R.string.button_add)) }
                }
            }
        }
    }
}

@Composable
fun EditSimpleItemDialog(
    item: SimpleItem,
    title: String,
    onDismiss: () -> Unit,
    onSave: (SimpleItem) -> Unit,
    onDelete: () -> Unit,
    isVehicle: Boolean
) {
    var text by remember(item) { mutableStateOf(item.title) }
    var subtitle by remember(item) { mutableStateOf(item.subtitle ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.favourites_delete_item), tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (isVehicle) stringResource(R.string.favourites_license_plate_label) else stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isVehicle) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text(stringResource(R.string.favourites_car_model_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(item.copy(title = text, subtitle = if (isVehicle) subtitle else null)) }) { Text(stringResource(R.string.button_save)) }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericGroupedList(
    groupedItems: Map<Char, List<SimpleItem>>,
    onEdit: (SimpleItem) -> Unit,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val groupIndexes = groupedItems.mapValues { entry ->
        var index = 0
        for ((key, value) in groupedItems) {
            if (key == entry.key) break
            index += value.size + 1
        }
        index
    }

    if (groupedItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.favourites_no_items), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                groupedItems.forEach { (header, items) ->
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = header.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(items) { item ->
                        SimpleListItem(
                            item = item,
                            onEdit = { onEdit(item) },
                            isSelected = item.id == selectedId,
                            onSelect = { onSelect(item.id) }
                        )
                    }
                }
            }

            AlphabetSidebar(
                groupedKeys = groupedItems.keys,
                onLetterClick = { letter ->
                    groupIndexes[letter]?.let { index ->
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun SimpleListItem(
    item: SimpleItem,
    onEdit: () -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onEdit),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedPlaceList(
    groupedFavorites: Map<Char, List<SavedPlace>>,
    onEditPlace: (SavedPlace) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val groupIndexes = groupedFavorites.mapValues { entry ->
        var index = 0
        for ((key, value) in groupedFavorites) {
            if (key == entry.key) break
            index += value.size + 1
        }
        index
    }

    if (groupedFavorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "No saved Favourites",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.favourites_no_items),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                groupedFavorites.forEach { (header, items) ->
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = header.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    itemsIndexed(items) { _, place ->
                        PlaceItem(
                            place = place,
                            onEdit = { onEditPlace(place) }
                        )
                    }
                }
            }

            AlphabetSidebar(
                groupedKeys = groupedFavorites.keys,
                onLetterClick = { letter ->
                    groupIndexes[letter]?.let { index ->
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AlphabetSidebar(
    groupedKeys: Set<Char>,
    onLetterClick: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ('A'..'Z').forEach { letter ->
            val isEnabled = groupedKeys.contains(letter)
            Text(
                text = letter.toString(),
                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(enabled = isEnabled) { onLetterClick(letter) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}


@Composable
fun PlaceItem(place: SavedPlace, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val addressParts = place.address.split(",", limit = 2)
            val street = addressParts.getOrNull(0)?.trim() ?: place.address
            val city = addressParts.getOrNull(1)?.trim() ?: ""

            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = street,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (city.isNotEmpty()) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
