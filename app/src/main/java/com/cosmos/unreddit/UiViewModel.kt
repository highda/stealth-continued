package com.cosmos.unreddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.extension.updateValue
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltViewModel
class UiViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val leftHandedMode: Flow<Boolean> = preferencesRepository
        .getLeftHandedMode()
        .distinctUntilChanged()

    private val _navigationVisibility = MutableStateFlow(true)
    val navigationVisibility: StateFlow<Boolean> = _navigationVisibility

    fun setNavigationVisibility(visible: Boolean) {
        _navigationVisibility.updateValue(visible)
    }

    fun showNsfw() {
        viewModelScope.launch { preferencesRepository.setShowNsfw(true) }
    }
}
