package dev.imanuel.abfuhr.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import dev.imanuel.abfuhr.Screens

@Composable
fun AppNavigationBar(
    activeScreen: Screens,
    navController: NavController
) {
    NavigationBar {
        NavigationBarItem(
            selected = activeScreen == Screens.Pickup,
            onClick = { navController.navigate(Screens.Pickup.name) },
            icon = {
                Icon(
                    imageVector = if (activeScreen == Screens.Pickup) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = "Abfuhrtermine"
                )
            },
            label = {
                Text("Termine")
            }
        )
        NavigationBarItem(
            selected = activeScreen == Screens.WasteAbc,
            onClick = { navController.navigate(Screens.WasteAbc.name) },
            icon = {
                Icon(
                    imageVector = if (activeScreen == Screens.WasteAbc) Icons.Filled.Recycling else Icons.Outlined.Recycling,
                    contentDescription = "Abfall ABC"
                )
            },
            label = {
                Text("Abfall ABC")
            }
        )
        NavigationBarItem(
            selected = activeScreen == Screens.Locations,
            onClick = { navController.navigate(Screens.Locations.name) },
            icon = {
                Icon(
                    imageVector = if (activeScreen == Screens.Locations) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                    contentDescription = "Standorte"
                )
            },
            label = {
                Text("Standorte")
            }
        )
    }
}