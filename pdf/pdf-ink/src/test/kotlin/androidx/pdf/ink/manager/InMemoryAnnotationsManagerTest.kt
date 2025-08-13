/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.ink.manager

import android.net.Uri
import androidx.pdf.ink.FakeEditablePdfDocument
import androidx.pdf.ink.util.createDummyUri
import com.google.common.truth.Truth.assertThat
import createStampAnnotationWithPath
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InMemoryAnnotationsManagerTest {

    private lateinit var fakeUri: Uri
    private lateinit var fakeDocument: FakeEditablePdfDocument
    private lateinit var annotationsManager: InMemoryAnnotationsManager

    @Before
    fun setUp() {
        fakeUri = createDummyUri("test.pdf")
        fakeDocument = FakeEditablePdfDocument(uri = fakeUri, pageCount = 10)
        annotationsManager = InMemoryAnnotationsManager(fakeDocument)
    }

    @Test
    fun getAnnotationsForPage_noAnnotations_returnsEmptyList() = runTest {
        val annotations = annotationsManager.getAnnotationsForPage(0)
        assertThat(annotations).isEmpty()
    }

    @Test
    fun getAnnotationsForPage_existingAnnotations_returnsAnnotationsFromDocument() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        val annotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].editId.pageNum).isEqualTo(0)
    }

    @Test
    fun getAnnotationsForPage_invokeGetTwice_fetchesOnlyOnce() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        fakeDocument.addAnnotationToPage(1, existingAnnotation)

        // Call twice
        annotationsManager.getAnnotationsForPage(1)
        val annotations = annotationsManager.getAnnotationsForPage(1)

        assertThat(annotations).hasSize(1)
        assertThat(fakeDocument.getAnnotationsForPageCallCount[1]).isEqualTo(1)
    }

    @Test
    fun addAnnotation_addsToDraftState_andIsReturnedByGetAnnotations() = runTest {
        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId = annotationsManager.addAnnotation(newAnnotation)
        val annotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations).hasSize(1)
        assertThat(annotations[0].editId).isEqualTo(editId)
        assertThat(fakeDocument.getAnnotationsForPageCallCount[0]).isEqualTo(1)
    }

    @Test
    fun addAnnotation_existingAndAdded_returnsBoth() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, existingAnnotation)

        // Fetch existing first to populate cache and draft
        annotationsManager.getAnnotationsForPage(0)

        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        annotationsManager.addAnnotation(newAnnotation)

        val allAnnotations = annotationsManager.getAnnotationsForPage(0)

        assertThat(allAnnotations).hasSize(2)
    }

    @Test
    fun addAnnotation_returnsUniqueEditId() {
        val annotation1 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val annotation2 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        val editId1 = annotationsManager.addAnnotation(annotation1)
        val editId2 = annotationsManager.addAnnotation(annotation2)

        assertThat(editId1).isNotNull()
        assertThat(editId2).isNotNull()
        assertThat(editId1).isNotEqualTo(editId2)
    }

    @Test
    fun getFullAnnotationStateSnapshot_empty_returnsEmptyState() = runTest {
        val snapshot = annotationsManager.getFullAnnotationStateSnapshot()
        assertThat(snapshot.edits).isEmpty()
    }

    @Test
    fun getFullAnnotationStateSnapshot_withAnnotations_returnsCorrectState() = runTest {
        val existingAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val newAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)

        fakeDocument.addAnnotationToPage(0, existingAnnotation)
        val existingAnnots = annotationsManager.getAnnotationsForPage(0)
        val editId = annotationsManager.addAnnotation(newAnnotation)

        val snapshot = annotationsManager.getFullAnnotationStateSnapshot()
        val allEdits = snapshot.edits

        assertThat(existingAnnots).hasSize(1)
        assertThat(allEdits.size).isEqualTo(1) // Only single entry in the map
        assertThat(allEdits[0]).isNotNull()
        assertThat(allEdits[0]).hasSize(2)
        assertThat(allEdits[0]!!.map { it.editId }).contains(editId)
    }

    @Test
    fun getAnnotationsForPage_multiplePages_annotationsAreSeparate() = runTest {
        val annotationPage0 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val annotationPage1 = createStampAnnotationWithPath(pageNum = 1, pathSize = 10)
        val newAnnotationPage0 = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        fakeDocument.addAnnotationToPage(0, annotationPage0)
        fakeDocument.addAnnotationToPage(1, annotationPage1)

        // Get page 0, then add to page 0
        val annotations0Initial = annotationsManager.getAnnotationsForPage(0)
        val editId = annotationsManager.addAnnotation(newAnnotationPage0)

        // Get page 1
        val annotations1 = annotationsManager.getAnnotationsForPage(1)
        val annotations0Final = annotationsManager.getAnnotationsForPage(0)

        assertThat(annotations0Initial.size).isEqualTo(1)
        assertThat(annotations1.size).isEqualTo(1)
        assertThat(annotations0Final.size).isEqualTo(2)
        assertThat(annotations0Final.map { it.editId.value }).contains(editId.value)
    }
}
