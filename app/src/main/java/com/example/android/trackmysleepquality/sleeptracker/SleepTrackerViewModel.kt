package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        private var tonight = MutableLiveData<SleepNight?>()
        private val nights = database.getAllNights()

        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }

        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
        val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        init {
                initializeTonight()
        }

        private fun initializeTonight() {
                viewModelScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        private suspend fun getTonightFromDatabase(): SleepNight? {
                var night = database.getTonight()
                if (night?.endTimeMilli != night?.startTimeMilli) {
                        night = null
                }
                return night
        }

        private suspend fun clear() {
                database.clear()
        }

        private suspend fun update(night: SleepNight) {
                database.update(night)
        }

        private suspend fun insert(night: SleepNight) {
                database.insert(night)
        }

        fun onStartTracking() {
                viewModelScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDatabase()
                }
        }

        fun onStopTracking() {
                viewModelScope.launch {
                        val oldNight = tonight.value ?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight
                }
        }

        fun onClear() {
                viewModelScope.launch {
                        clear()
                        tonight.value = null
                }
        }

}
