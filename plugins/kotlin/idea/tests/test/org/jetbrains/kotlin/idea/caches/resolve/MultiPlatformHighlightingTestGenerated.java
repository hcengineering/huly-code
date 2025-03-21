// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.caches.resolve;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("idea/tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("testData/multiModuleHighlighting/multiplatform")
public class MultiPlatformHighlightingTestGenerated extends AbstractMultiPlatformHighlightingTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K1;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("actualizedSupertype")
    public void testActualizedSupertype() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/actualizedSupertype/");
    }

    @TestMetadata("additionalMembersInPlatformInterface")
    public void testAdditionalMembersInPlatformInterface() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/additionalMembersInPlatformInterface/");
    }

    @TestMetadata("basic")
    public void testBasic() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/basic/");
    }

    @TestMetadata("catchHeaderExceptionInPlatformModule")
    public void testCatchHeaderExceptionInPlatformModule() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/catchHeaderExceptionInPlatformModule/");
    }

    @TestMetadata("completionHandlexCoroutines")
    public void testCompletionHandlexCoroutines() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/completionHandlexCoroutines/");
    }

    @TestMetadata("contracts")
    public void testContracts() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/contracts/");
    }

    @TestMetadata("depends")
    public void testDepends() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/depends/");
    }

    @TestMetadata("differentJvmImpls")
    public void testDifferentJvmImpls() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/differentJvmImpls/");
    }

    @TestMetadata("headerClass")
    public void testHeaderClass() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerClass/");
    }

    @TestMetadata("headerClassImplTypealias")
    public void testHeaderClassImplTypealias() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerClassImplTypealias/");
    }

    @TestMetadata("headerFunUsesStdlibInSignature")
    public void testHeaderFunUsesStdlibInSignature() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerFunUsesStdlibInSignature/");
    }

    @TestMetadata("headerFunctionProperty")
    public void testHeaderFunctionProperty() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerFunctionProperty/");
    }

    @TestMetadata("headerPartiallyImplemented")
    public void testHeaderPartiallyImplemented() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerPartiallyImplemented/");
    }

    @TestMetadata("headerWithoutImplForBoth")
    public void testHeaderWithoutImplForBoth() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/headerWithoutImplForBoth/");
    }

    @TestMetadata("internal")
    public void testInternal() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/internal/");
    }

    @TestMetadata("internalDependencyFromTests")
    public void testInternalDependencyFromTests() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/internalDependencyFromTests/");
    }

    @TestMetadata("internalInheritanceToCommon")
    public void testInternalInheritanceToCommon() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/internalInheritanceToCommon/");
    }

    @TestMetadata("javaUsesPlatformFacade")
    public void testJavaUsesPlatformFacade() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/javaUsesPlatformFacade/");
    }

    @TestMetadata("jvmKotlinReferencesCommonKotlinThroughJava")
    public void testJvmKotlinReferencesCommonKotlinThroughJava() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/jvmKotlinReferencesCommonKotlinThroughJava/");
    }

    @TestMetadata("jvmKotlinReferencesCommonKotlinThroughJavaDifferentJvmImpls")
    public void testJvmKotlinReferencesCommonKotlinThroughJavaDifferentJvmImpls() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/jvmKotlinReferencesCommonKotlinThroughJavaDifferentJvmImpls/");
    }

    @TestMetadata("jvmNameInCommon")
    public void testJvmNameInCommon() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/jvmNameInCommon/");
    }

    @TestMetadata("multifileFacade")
    public void testMultifileFacade() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/multifileFacade/");
    }

    @TestMetadata("nestedClassWithoutImpl")
    public void testNestedClassWithoutImpl() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/nestedClassWithoutImpl/");
    }

    @TestMetadata("platformTypeAliasInterchangebleWithAliasedClass")
    public void testPlatformTypeAliasInterchangebleWithAliasedClass() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/platformTypeAliasInterchangebleWithAliasedClass/");
    }

    @TestMetadata("sealedTypeAlias")
    public void testSealedTypeAlias() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/sealedTypeAlias/");
    }

    @TestMetadata("suppressHeaderWithoutImpl")
    public void testSuppressHeaderWithoutImpl() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/suppressHeaderWithoutImpl/");
    }

    @TestMetadata("suspend")
    public void testSuspend() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/suspend/");
    }

    @TestMetadata("transitive")
    public void testTransitive() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/transitive/");
    }

    @TestMetadata("triangle")
    public void testTriangle() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/triangle/");
    }

    @TestMetadata("triangleWithDependency")
    public void testTriangleWithDependency() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/triangleWithDependency/");
    }

    @TestMetadata("typeAliasedParameter")
    public void testTypeAliasedParameter() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/typeAliasedParameter/");
    }

    @TestMetadata("typeAliasedSam")
    public void testTypeAliasedSam() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/typeAliasedSam/");
    }

    @TestMetadata("useAppendable")
    public void testUseAppendable() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/useAppendable/");
    }

    @TestMetadata("useCorrectBuiltInsForCommonModule")
    public void testUseCorrectBuiltInsForCommonModule() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/useCorrectBuiltInsForCommonModule/");
    }

    @TestMetadata("usePlatformSpecificMember")
    public void testUsePlatformSpecificMember() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/usePlatformSpecificMember/");
    }

    @TestMetadata("withOverrides")
    public void testWithOverrides() throws Exception {
        runTest("testData/multiModuleHighlighting/multiplatform/withOverrides/");
    }
}
