Registration Dialog still I have to manually resize the window. Signup button also not presented on the registration dialog page.

I can see some dashes in the text field placeholder text of Login (Sign in) page.

Still not satisfied with the dashboard section for very users. Looking ugly. The statistics are not properly presented.

The contents overflowing needs to scroll left and right which is not the ideal one.

Many icons (Circulation, Users) are not visible.

The dark theme is applied to main page not other pages. Color scheme for both light and dark theme is bad some text and some elements are not properly visible.

Still the moon icon is not visible after turning the light theme into dark.

User can also update their own profile, provide them a option to do so.

The action buttons are not placed properly center.

If the reason is too long, it cannot be able to display it fully. We have to think of strategy to fix that.

I cannot test the overdue feature. Every test should be on the UI right?

You are treating the software in single computer. You are giving Config Dialog while the system is new. But it is for the Librarians, should not ask the user, the user is prompted to ask the choose the library from dropdown menu (like a datalist), the user can able to type the library name it should filter  the result like a html datalist.

Library Configuration Dialog is only needed for the librarians.


Successfully wrote object to: /tmp/lib-test-16334202031507421154/test_book.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/test_book.ser
Successfully wrote object to: /tmp/lib-test-16334202031507421154/test_config.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/test_config.ser
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: admin01
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: admin01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: user01
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: user01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: tom001
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: tom001
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: uma001
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: uma001
Successfully wrote object to: data/users_db.ser (atomic)

expected: <2> but was: <14>
Comparison Failure:
Expected :2
Actual   :14
<Click to see difference>

org.opentest4j.AssertionFailedError: expected: <2> but was: <14>
at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.getAllUsers(LibraryTestSuite.java:1266)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)

Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: sam001
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: sam001
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB updateUser
Successfully wrote object to: data/users_db.ser (atomic)
INFO: User updated successfully: sam001
Apr 18, 2026 11:56:30 AM com.example.services.UserService updateUser
INFO: User updated successfully: sam001
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.services.UserService getUserById
SEVERE: Failed to retrieve user: totally_unknown_xyz
com.example.exceptions.UserException: User not found: totally_unknown_xyz
at com.example.application/com.example.services.UserService.getUserById(UserService.java:105)
at com.example.application/com.example.services.UserService.getUserRole(UserService.java:208)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.getRoleUnknown(LibraryTestSuite.java:1229)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:63)
at com.intellij.junit5.JUnit5TestRunnerHelper.execute(JUnit5TestRunnerHelper.java:134)
at com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:70)
at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:244)
at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:65)

Apr 18, 2026 11:56:30 AM com.example.services.UserService getUserRole
WARNING: Failed to get role for user: totally_unknown_xyz
com.example.exceptions.UserException: User not found: totally_unknown_xyz
at com.example.application/com.example.services.UserService.getUserById(UserService.java:105)
at com.example.application/com.example.services.UserService.getUserRole(UserService.java:208)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.getRoleUnknown(LibraryTestSuite.java:1229)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:63)
at com.intellij.junit5.JUnit5TestRunnerHelper.execute(JUnit5TestRunnerHelper.java:134)
at com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:70)
at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:244)
at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:65)

Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: vic001
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: vic001
Successfully read object from: data/users_db.ser
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database

expected: <1> but was: <14>
Comparison Failure:
Expected :1
Actual   :14
<Click to see difference>

org.opentest4j.AssertionFailedError: expected: <1> but was: <14>
at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.userCount(LibraryTestSuite.java:1273)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)

Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: peter01
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: peter01
Apr 18, 2026 11:56:30 AM com.example.services.UserService login
INFO: Authentication attempt for user: peter01 - FAILURE
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: mike01
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: mike01
Successfully read object from: data/users_db.ser
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Successfully read object from: data/users_db.ser
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: quin01
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: quin01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: rose01
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: rose01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB removeUser
INFO: User removed successfully: rose01
Apr 18, 2026 11:56:30 AM com.example.services.UserService deleteUser
INFO: User deleted successfully: rose01
Successfully read object from: data/users_db.ser
Successfully wrote object to: data/users_db.ser (atomic)
Successfully wrote object to: data/users_db.ser (atomic)
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService getUserById
SEVERE: Failed to retrieve user: rose01
com.example.exceptions.UserException: User not found: rose01
at com.example.application/com.example.services.UserService.getUserById(UserService.java:105)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.lambda$deleteUser$0(LibraryTestSuite.java:1247)
at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
at com.example.application/com.example.test.LibraryTestSuite$UserServiceTests.deleteUser(LibraryTestSuite.java:1247)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:63)
at com.intellij.junit5.JUnit5TestRunnerHelper.execute(JUnit5TestRunnerHelper.java:134)
at com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:70)
at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:244)
at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:65)

Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: olivia01
Successfully wrote object to: data/users_db.ser (atomic)
Apr 18, 2026 11:56:30 AM com.example.services.UserService createUser
INFO: User created successfully: olivia01
Apr 18, 2026 11:56:30 AM com.example.services.UserService login
INFO: Authentication attempt for user: olivia01 - SUCCESS
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user temp01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: temp01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 1 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user igor01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: igor01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user igor01 to ADMIN
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: igor01

org.opentest4j.AssertionFailedError: Expected com.example.exceptions.UserException to be thrown, but nothing was thrown.

	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
	at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
	at com.example.application/com.example.test.LibraryTestSuite$UsersDBTests.duplicateUser(LibraryTestSuite.java:1055)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)

Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Successfully read object from: data/users_db.ser
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user helen01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: helen01
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user lisa01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: lisa01
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB updateUser
INFO: User updated successfully: lisa01
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user jane02 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: jane02
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: sole_admin
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB removeUser
SEVERE: Failed to remove user: sole_admin
com.example.exceptions.UserException: At least one administrator must remain in the system
at com.example.application/com.example.entities.UsersDB.removeUser(UsersDB.java:301)
at com.example.application/com.example.test.LibraryTestSuite$UsersDBTests.lambda$removeLastAdminThrows$0(LibraryTestSuite.java:1123)
at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
at com.example.application/com.example.test.LibraryTestSuite$UsersDBTests.removeLastAdminThrows(LibraryTestSuite.java:1123)
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:218)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:214)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:139)
at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
at org.junit.platform.launcher.core.SessionPerRequestLauncher.execute(SessionPerRequestLauncher.java:63)
at com.intellij.junit5.JUnit5TestRunnerHelper.execute(JUnit5TestRunnerHelper.java:134)
at com.intellij.junit5.JUnit5IdeaTestRunner.startRunnerWithArgs(JUnit5IdeaTestRunner.java:70)
at com.intellij.rt.junit.IdeaTestRunner$Repeater$1.execute(IdeaTestRunner.java:38)
at com.intellij.rt.execution.junit.TestsRepeater.repeat(TestsRepeater.java:11)
at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:35)
at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:244)
at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:65)

Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: adm001
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: adm002
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB removeUser
INFO: User removed successfully: adm001
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user jane01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: jane01
Successfully read object from: data/users_db.ser
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user first_user to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: first_user
Successfully read object from: data/users_db.ser
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB getInstance
INFO: Loaded existing UsersDB with 14 users
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB setAutoSave
INFO: Auto-save set to: false
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB clearAllUsers
WARNING: Cleared all 14 users from database
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB ensureAdminUserExists
INFO: Promoted first user karl01 to ADMIN
Apr 18, 2026 11:56:30 AM com.example.entities.UsersDB addUser
INFO: User object added successfully: karl01
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000006 - Test Title 1000000006
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 2 copies of book 1000000006 to user testuser01
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000011 - Test Title 1000000011
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000008 - Title
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 2 copies of book 1000000008 to user testuser01
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000099 - Concurrency Book
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000099 to user concurrent4
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000099 to user concurrent1
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000099 to user concurrent3
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000099 to user concurrent0
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000099 to user concurrent2
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000002 - Test Title 1000000002
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000010 - Test Title 1000000010
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000010 to user testuser01
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000016 - Fine Test
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000016 to user testuser01
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000012 - Test Title 1000000012
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000013 - Test Title 1000000013

expected: <2> but was: <3>
Comparison Failure:
Expected :2
Actual   :3
<Click to see difference>

org.opentest4j.AssertionFailedError: expected: <2> but was: <3>
at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
at org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
at org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
at org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
at com.example.application/com.example.test.LibraryTestSuite$BooksDBTests.searchEmpty(LibraryTestSuite.java:941)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)

Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000001 - Test Title 1000000001
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000007 - Title
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000004 - Test Title 1000000004
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1000000004 - Test Title 1000000004
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000003 - Test Title 1000000003
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB modifyBook
INFO: Book updated: 1000000003 - Updated Title
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000009 - Test Title 1000000009
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 2 copies of book 1000000009 to user testuser01
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB returnBook
INFO: Returned 2 copies of book 1000000009 from user testuser01
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000014 - Java Programming
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000015 - Python Basics
Successfully read object from: data/books_db.ser
Successfully read object from: data/issued_books.ser
Successfully read object from: data/borrower_details.ser
Successfully read object from: data/issue_records.ser
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB getInstance
INFO: Loaded existing BooksDB with 3 books
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 1234567890 - Java Fundamentals
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB removeBook
INFO: Book removed: 0123456789 - Python Mastery
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB addBook
INFO: Book added: 1000000005 - Test Title 1000000005
Apr 18, 2026 11:56:30 AM com.example.entities.BooksDB issueBook
INFO: Issued 1 copies of book 1000000005 to user testuser01
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_basic.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/ds_basic.ser
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_concurrent.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/ds_concurrent.ser
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_overwrite.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_overwrite.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/ds_overwrite.ser
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_size.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_exists.ser (atomic)
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_map.ser (atomic)
Successfully read object from: /tmp/lib-test-16334202031507421154/ds_map.ser
INFO: File does not exist: /tmp/lib-test-16334202031507421154/does_not_exist.ser
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_backup.ser (atomic)
Created backup: /tmp/lib-test-16334202031507421154/ds_backup.ser.backup.1776493590841
Successfully wrote object to: /tmp/lib-test-16334202031507421154/ds_delete.ser (atomic)

Process finished with exit code 255





















Username is not used as primary key because when I register the username as "Librarian" and the role as Librarian. It says request sent to Admin but when I gave the same username for user, it allows.

Create Account Dialog is not opened properly, I should manually expand the windows

If I click any option on settings, the empty window is opening.

Many icons are missing.

When I click dark theme button, the button is not visible to the eyes.

The dashboard statistics are not properly framed. Also the back arrow is not placed.

There is no button for sign up in registration dialog.

When I hit approve button for the Librarian Addition, the UI is not updating, updated after only I reopened the dialog.

You asked the admin to give the reason(optional) but it is not presented to the user.

How Librarian and Admin can request a book using the librarian and admin type of account. It is not making any sense.

How can a librarian can delete the admin account

Provide the user type of account to modify their profile. Why it is missing btw?

What should I infer ... in many buttons and places?