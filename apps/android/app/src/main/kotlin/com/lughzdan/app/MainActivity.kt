package com.lughzdan.app
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.lughzdan.data.LocalGameRepository
import com.lughzdan.db.LughzdanDatabase
import com.lughzdan.domain.ProgressRepository
import com.lughzdan.games.GameApp
import com.lughzdan.games.GameViewModel
import com.lughzdan.preferences.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
@HiltAndroidApp class LughzdanApplication:Application()
@Module @InstallIn(SingletonComponent::class) object AppModule {
 @Provides @Singleton fun database(@ApplicationContext context:Context)=Room.databaseBuilder(context,LughzdanDatabase::class.java,"lughzdan.db").addMigrations(LughzdanDatabase.MIGRATION_1_2).build()
 @Provides @Singleton fun repository(@ApplicationContext context:Context,db:LughzdanDatabase):ProgressRepository=LocalGameRepository(context,db,BuildConfig.DEBUG)
 @Provides @Singleton fun settings(@ApplicationContext context:Context)=SettingsStore(context)
}
@AndroidEntryPoint class MainActivity:ComponentActivity() {
 @Inject lateinit var repository:ProgressRepository
 @Inject lateinit var settings:SettingsStore
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
  val factory=object:ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=GameViewModel(repository,settings) as T }
  setContent { val vm:GameViewModel=viewModel(factory=factory);GameApp(vm) }
 }
}
