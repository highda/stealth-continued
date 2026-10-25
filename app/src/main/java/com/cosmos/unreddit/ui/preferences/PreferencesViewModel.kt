package com.cosmos.unreddit.ui.preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.model.preferences.UiPreferences
import com.cosmos.unreddit.data.remote.instance.Instance
import com.cosmos.unreddit.data.remote.instance.InstanceManager
import com.cosmos.unreddit.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    instanceManager: InstanceManager
) : ViewModel() {

    val nightMode: SharedFlow<Int> = preferencesRepository.getNightMode()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val leftHandedMode: Flow<Boolean> = preferencesRepository.getLeftHandedMode()

    val postLayout: Flow<UiPreferences.PostLayout> = preferencesRepository.getPostLayout()

    val showNsfw: Flow<Boolean> = preferencesRepository.getShowNsfw()

    val showNsfwPreview: Flow<Boolean> = preferencesRepository.getShowNsfwPreview()

    val showSpoilerPreview: Flow<Boolean> = preferencesRepository.getShowSpoilerPreview()

    val instance: StateFlow<Instance?> = instanceManager.instance

    val privacyEnhancerEnabled: SharedFlow<Boolean> = preferencesRepository
        .getPrivacyEnhancerEnabled()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    fun setNightMode(nightMode: Int) {
        viewModelScope.launch {
            preferencesRepository.setNightMode(nightMode)
        }
    }

    fun setLeftHandedMode(leftHandedMode: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLeftHandedMode(leftHandedMode)
        }
    }

    fun setPostLayout(postLayout: UiPreferences.PostLayout) {
        viewModelScope.launch {
            preferencesRepository.setPostLayout(postLayout)
        }
    }

    fun setShowNsfw(showNsfw: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowNsfw(showNsfw)
        }
    }

    fun setShowNsfwPreview(showNsfwPreview: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowNsfwPreview(showNsfwPreview)
        }
    }

    fun setShowSpoilerPreview(showSpoilerPreview: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSpoilerPreview(showSpoilerPreview)
        }
    }
}
