package com.bilibili.lib.blrouter.plugin.internal.lib

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails
import java.io.File

abstract class ApiTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    abstract val primaryInput: File

    override fun transform(outputs: TransformOutputs) {
        if (primaryInput.name == JAR_NAME) {
            outputs.file(primaryInput)
        }
    }
}


class AllDefault : AttributeDisambiguationRule<String> {
    override fun execute(t: MultipleCandidatesDetails<String>) {
        if (t.consumerValue == null && t.candidateValues.contains(ALL)) {
            t.closestMatch(ALL)
        }
    }
}