// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExcludeCodeGen

@Target(AnnotationTarget.CLASS)
annotation class ConstList(
    val name: String
)

@Target(AnnotationTarget.FIELD)
annotation class ForceCodeGenType(
    val type: KClass<*>
)

// Treats this endpoint as a plain GET request, even if it e.g. produces json
@Target(AnnotationTarget.FUNCTION)
annotation class ForcePlainGet
