package com.bilibili.brouter.plugin.internal.dependency

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import java.io.File

abstract class XmlToManifestTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    abstract val primaryInput: File

    override fun transform(outputs: TransformOutputs) {
        if (primaryInput.exists() && primaryInput.name == "AndroidManifest.xml") {
            outputs.file(primaryInput)
        }
    }
}