package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HazeAlertEntity
import com.example.data.location.PenangStationsData
import com.example.data.model.*
import com.example.data.repository.HazeRepository
import com.example.data.repository.HealthRecommendationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HazeUiState(
    val isLoading: Boolean = true,
    val reading: AirQualityReading? = null,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val recommendations: List<HealthRecommendation> = emptyList(),
    val selectedZone: PenangStation? = null,
    val allStations: List<PenangStation> = PenangStationsData.STATIONS,
    val stationComparisons: List<Pair<PenangStation, Int>> = emptyList(),
    val userProfile: UserHealthProfile = UserHealthProfile(),
    val alertsList: List<HazeAlertEntity> = emptyList(),
    val emergencyContacts: List<PenangEmergencyContact> = PenangStationsData.EMERGENCY_CONTACTS,
    val activeAlertBanner: String? = null,
    val isSimulatedSpike: Boolean = false,
    val simulatedSpikeValue: Int? = null,
    val selectedNavTab: Int = 0,
    val isDarkTheme: Boolean = true,
    // Historical Data state
    val historyAnalytics: HistoricalAnalytics? = null,
    val selectedHistoryRange: DateRangeType = DateRangeType.LAST_30_DAYS,
    val selectedHistoryStation: PenangStation? = null,
    val customHistoryStartMillis: Long? = null,
    val customHistoryEndMillis: Long? = null,
    val isHistoryLoading: Boolean = false,
    // Map State
    val selectedMapStation: PenangStation? = null,
    val isHeatmapEnabled: Boolean = true,
    val mapDistrictFilter: String = "ALL", // "ALL", "ISLAND", "MAINLAND"
    val isGoogleMapMode: Boolean = true,
    val googleMapTypeCode: String = "m" // "m" for Roadmap, "y" for Satellite Hybrid, "p" for Terrain
)

class HazeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HazeRepository(application)

    private val _uiState = MutableStateFlow(HazeUiState())
    val uiState: StateFlow<HazeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeAlerts()
        loadHistoricalData()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            repository.allAlerts.collect { alerts ->
                _uiState.update { it.copy(alertsList = alerts) }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val (reading, forecast) = repository.fetchCurrentAirQuality(
                    forcedZone = state.selectedZone,
                    userProfile = state.userProfile,
                    simulatedSpikeApi = if (state.isSimulatedSpike) state.simulatedSpikeValue else null
                )

                val recs = HealthRecommendationEngine.generateRecommendations(
                    hazeLevel = reading.hazeLevel,
                    apiValue = reading.apiValue,
                    profile = state.userProfile,
                    stationName = reading.station.name
                )

                val comparisons = repository.getStationComparison()

                val custom = state.userProfile.customThresholds
                val banner = if (custom.isCustomEnabled && custom.shouldTriggerAlert(reading.apiValue)) {
                    custom.getTriggerReason(reading.apiValue, reading.station.name)
                } else if (reading.apiValue >= state.userProfile.alertThresholdApi) {
                    "ALERT: ${reading.station.name} haze API reached ${reading.apiValue} (${reading.hazeLevel.title}). Automated health protocols active."
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reading = reading,
                        hourlyForecast = forecast,
                        recommendations = recs,
                        stationComparisons = comparisons,
                        activeAlertBanner = banner
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadHistoricalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryLoading = true) }
            try {
                val state = _uiState.value
                val analytics = repository.getHistoricalAnalytics(
                    rangeType = state.selectedHistoryRange,
                    stationId = state.selectedHistoryStation?.id,
                    customStartMillis = state.customHistoryStartMillis,
                    customEndMillis = state.customHistoryEndMillis
                )
                _uiState.update {
                    it.copy(
                        historyAnalytics = analytics,
                        isHistoryLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isHistoryLoading = false) }
            }
        }
    }

    fun selectHistoryRange(range: DateRangeType) {
        _uiState.update { it.copy(selectedHistoryRange = range) }
        loadHistoricalData()
    }

    fun selectHistoryStation(station: PenangStation?) {
        _uiState.update { it.copy(selectedHistoryStation = station) }
        loadHistoricalData()
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _uiState.update {
            it.copy(
                selectedHistoryRange = DateRangeType.CUSTOM,
                customHistoryStartMillis = startMillis,
                customHistoryEndMillis = endMillis
            )
        }
        loadHistoricalData()
    }

    fun selectMapStation(station: PenangStation?) {
        _uiState.update { it.copy(selectedMapStation = station) }
    }

    fun toggleHeatmap() {
        _uiState.update { it.copy(isHeatmapEnabled = !it.isHeatmapEnabled) }
    }

    fun setMapDistrictFilter(filter: String) {
        _uiState.update { it.copy(mapDistrictFilter = filter) }
    }

    fun toggleMapRendererMode() {
        _uiState.update { it.copy(isGoogleMapMode = !it.isGoogleMapMode) }
    }

    fun setGoogleMapTypeCode(code: String) {
        _uiState.update { it.copy(googleMapTypeCode = code) }
    }

    fun selectZone(zone: PenangStation?) {
        _uiState.update { it.copy(selectedZone = zone) }
        loadData()
    }

    fun selectNavTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedNavTab = tabIndex) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun updateProfile(newProfile: UserHealthProfile) {
        _uiState.update { it.copy(userProfile = newProfile) }
        val currentReading = _uiState.value.reading
        if (currentReading != null) {
            val recs = HealthRecommendationEngine.generateRecommendations(
                hazeLevel = currentReading.hazeLevel,
                apiValue = currentReading.apiValue,
                profile = newProfile,
                stationName = currentReading.station.name
            )
            _uiState.update { it.copy(recommendations = recs) }
        }
    }

    fun updateCustomAlertThresholds(
        goodLimit: Int,
        moderateLimit: Int,
        unhealthyLimit: Int,
        notifyModerate: Boolean,
        notifyUnhealthy: Boolean,
        notifyHazardous: Boolean,
        isCustomEnabled: Boolean = true
    ) {
        val custom = CustomAlertThresholds(
            goodLimit = goodLimit,
            moderateLimit = moderateLimit,
            unhealthyLimit = unhealthyLimit,
            notifyOnModerate = notifyModerate,
            notifyOnUnhealthy = notifyUnhealthy,
            notifyOnHazardous = notifyHazardous,
            isCustomEnabled = isCustomEnabled
        )
        val updated = _uiState.value.userProfile.copy(
            customThresholds = custom,
            alertThresholdApi = moderateLimit
        )
        updateProfile(updated)
        loadData()
    }

    fun resetThresholdsToDefault() {
        val defaultThresholds = CustomAlertThresholds(
            goodLimit = 50,
            moderateLimit = 100,
            unhealthyLimit = 200,
            notifyOnModerate = false,
            notifyOnUnhealthy = true,
            notifyOnHazardous = true,
            isCustomEnabled = true
        )
        val updated = _uiState.value.userProfile.copy(
            customThresholds = defaultThresholds,
            alertThresholdApi = 100
        )
        updateProfile(updated)
        loadData()
    }

    fun updateAlertThreshold(threshold: Int) {
        val updated = _uiState.value.userProfile.copy(alertThresholdApi = threshold)
        updateProfile(updated)
    }

    fun triggerSimulatedSpike(apiValue: Int) {
        _uiState.update {
            it.copy(
                isSimulatedSpike = true,
                simulatedSpikeValue = apiValue
            )
        }
        loadData()
    }

    fun resetSimulatedSpike() {
        _uiState.update {
            it.copy(
                isSimulatedSpike = false,
                simulatedSpikeValue = null
            )
        }
        loadData()
    }

    fun dismissBanner() {
        _uiState.update { it.copy(activeAlertBanner = null) }
    }

    fun markAlertRead(alertId: Long) {
        viewModelScope.launch {
            repository.markAlertRead(alertId)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }
}
