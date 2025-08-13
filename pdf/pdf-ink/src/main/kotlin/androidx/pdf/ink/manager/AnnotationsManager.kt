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

import androidx.pdf.annotation.draftstate.ImmutableAnnotationEditsDraftState
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData

/** Manages annotations for a PDF document. */
internal interface AnnotationsManager {
    /**
     * Retrieves all annotations for a given page number.
     *
     * @param pageNum The page number (0-indexed) to retrieve annotations for.
     * @return A list of [PdfAnnotationData] for the specified page.
     */
    suspend fun getAnnotationsForPage(pageNum: Int): List<PdfAnnotationData>

    /**
     * Adds a new annotation.
     *
     * @param annotation The [PdfAnnotation] to add.
     * @return The [EditId] assigned to the newly added annotation.
     */
    fun addAnnotation(annotation: PdfAnnotation): EditId

    /**
     * Returns an immutable snapshot of the current annotation state.
     *
     * This snapshot includes all edits and additions made to the annotations.
     *
     * @return An [ImmutableAnnotationEditsDraftState] representing the current state.
     */
    fun getFullAnnotationStateSnapshot(): ImmutableAnnotationEditsDraftState
}
