package com.lughzdan.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.data.LocalGameRepository
import com.lughzdan.db.LughzdanDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26],application=android.app.Application::class)
class ReleaseContentSmokeTest {
    @Test fun productionBundleAndImagesOpenOnAndroidEightWithoutFixtures() = runBlocking {
        assumeFalse(BuildConfig.DEBUG)
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository=LocalGameRepository(context,db,false)
            repository.initialize()
            val puzzles=repository.puzzles.first()
            assertEquals(120,puzzles.size)
            assertTrue(puzzles.none {it.fixtureOnly})
            val visual=puzzles.first {it.type=="four_pictures" && it.difficulty==1}
            val session=repository.start(visual.puzzleId)
            visual.payload.images.forEach { image ->
                assertTrue(repository.readImage(session,image.id)!!.isNotEmpty())
            }
        } finally {db.close()}
    }
}
