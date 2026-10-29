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
import com.lughzdan.data.*
import com.lughzdan.content.ManifestVerifier
import com.lughzdan.network.ContentApi
import kotlinx.serialization.json.*
import java.io.File
import java.util.Base64
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
@HiltAndroidApp class LughzdanApplication:Application(),ContentRuntimeOwner {
 @Inject lateinit var runtime:LocalContentRepository
 override val contentRuntime get()=runtime
}
@Module @InstallIn(SingletonComponent::class) object AppModule {
 @Provides @Singleton fun database(@ApplicationContext context:Context)=Room.databaseBuilder(context,LughzdanDatabase::class.java,"lughzdan.db").addMigrations(LughzdanDatabase.MIGRATION_1_2,LughzdanDatabase.MIGRATION_2_3).build()
 @Provides @Singleton fun content(@ApplicationContext context:Context,db:LughzdanDatabase,settings:SettingsStore):LocalContentRepository {
  val keys=if(context.assets.list("").orEmpty().contains("pack-public-keys.json")) context.assets.open("pack-public-keys.json").bufferedReader().use { reader->Json.parseToJsonElement(reader.readText()).jsonObject.mapValues {Base64.getDecoder().decode(it.value.jsonPrimitive.content)} } else emptyMap()
  val installer=PackInstaller(db,File(context.filesDir,"packs"),ManifestVerifier(keys),BuildConfig.VERSION_CODE)
  val api=BuildConfig.CONTENT_API_URL.takeIf {it.isNotBlank() && keys.isNotEmpty()}?.let(ContentApi::create)
  return LocalContentRepository(context,db,settings,installer,api)
 }
 @Provides @Singleton fun repository(@ApplicationContext context:Context,db:LughzdanDatabase,content:LocalContentRepository):ProgressRepository=LocalGameRepository(context,db,BuildConfig.DEBUG,recoverContent=content::initialize)
 @Provides @Singleton fun settings(@ApplicationContext context:Context)=SettingsStore(context)
}
@AndroidEntryPoint class MainActivity:ComponentActivity() {
 @Inject lateinit var repository:ProgressRepository
 @Inject lateinit var settings:SettingsStore
 @Inject lateinit var content:LocalContentRepository
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
  val factory=object:ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=GameViewModel(repository,settings,content) as T }
  setContent { val vm:GameViewModel=viewModel(factory=factory);GameApp(vm) }
 }
}
