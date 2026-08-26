package com.shellanddeploy.fpllive

import android.app.Application
import com.shellanddeploy.fpllive.data.api.FplApi
import com.shellanddeploy.fpllive.data.api.FplClient
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.api.FplRepositoryImpl
import com.shellanddeploy.fpllive.data.auth.AuthenticationRepository
import com.shellanddeploy.fpllive.data.auth.UnavailableAuthenticationRepository
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.data.db.FplDatabase
import com.shellanddeploy.fpllive.data.namesearch.HttpNameSearchRepository
import com.shellanddeploy.fpllive.data.namesearch.NameSearchRepository
import com.shellanddeploy.fpllive.data.namesearch.NoOpNameSearchRepository
import com.shellanddeploy.fpllive.notifications.ReminderScheduler
import com.shellanddeploy.fpllive.notifications.WorkManagerReminderScheduler

class FplApp : Application() {

    lateinit var repository: FplRepository
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var database: FplDatabase
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set
    lateinit var authentication: AuthenticationRepository
        private set
    lateinit var nameSearch: NameSearchRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val api: FplApi = FplClient.create(this)
        database = FplDatabase.build(this)
        repository = FplRepositoryImpl(api, database)
        settings = SettingsRepository(this)
        reminderScheduler = WorkManagerReminderScheduler(this)
        authentication = UnavailableAuthenticationRepository()
        nameSearch = if (BuildConfig.NAME_SEARCH_BASE_URL.isNotBlank()) {
            HttpNameSearchRepository(BuildConfig.NAME_SEARCH_BASE_URL)
        } else {
            NoOpNameSearchRepository
        }
    }
}
