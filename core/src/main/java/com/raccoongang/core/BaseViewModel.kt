package com.raccoongang.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import org.koin.java.KoinJavaComponent.inject

open class BaseViewModel : ViewModel(), DefaultLifecycleObserver