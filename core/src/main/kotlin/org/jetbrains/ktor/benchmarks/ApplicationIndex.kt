package org.jetbrains.ktor.benchmarks

import org.objectweb.asm.*
import java.io.*
import java.net.*
import java.nio.file.*
import java.util.zip.*
import kotlin.reflect.jvm.*

object ApplicationIndex {
    private val allInterestingClasses by lazy { init() }
    val exludedPackages = listOf("com.sun.", "java.", "javax.", "org.apache.", "kotlin.", "kotlinx.")

    private fun init(): List<Class<*>> {
        val urls = ApplicationIndex.javaClass.classLoader.allURLs() +
                (Thread.currentThread().contextClassLoader?.allURLs() ?: emptyList()) +
                ClassLoader.getSystemClassLoader().allURLs()

        return urls.filter { it.protocol == "file" }
                .map(URL::toURI)
                .map { Paths.get(it) }
                .filterNot { it.any { it.toString().startsWith("jdk") || it.toString().startsWith("jre") } }
                .map(Path::toAbsolutePath)
                .distinct()
                .flatMap { jarOrDir ->
                    visitPath(jarOrDir)
                }
    }

    private fun visitPath(jarOrDir: Path): List<Class<*>> {
        return if (Files.isDirectory(jarOrDir)) {
            visitDirectory(jarOrDir)
        } else if (Files.isRegularFile(jarOrDir)) {
            visitJar(jarOrDir)
        } else {
            emptyList()
        }
    }

    private fun visitDirectory(p: Path): List<Class<*>> {
        return p.toFile().walkTopDown()
                .filter { it.isDirectory || it.extension == "class" }
                .asSequence()
                .filter { it.isFile }
                .mapNotNull { classFile ->
                    classFile.inputStream().buffered().use { stream ->
                        ClassReader(stream).let { reader ->
                            if (check(reader)) {
                                Class.forName(reader.className.replace("/", "."), false, ApplicationIndex.javaClass.classLoader)
                            } else {
                                null
                            }
                        }
                    }
                }
                .toList()
    }

    private fun visitJar(p: Path): List<Class<*>> {
        return ZipFile(p.toFile()).use { z ->
            z.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.name.endsWith(".class") }
                    .filter { clazzEntry ->
                        check(ClassReader(z.getInputStream(clazzEntry)))
                    }
                    .map { Class.forName(it.name.replace("/", ".").removeSuffix(".class"), false, ApplicationIndex.javaClass.classLoader) }
                    .toList()
        }
    }

    //    private fun check(r: ClassReader) = ServerRule.ServerApplication::class.jvmName.replace(".", "/") in r.interfaces.orEmpty()
    private fun check(r: ClassReader): Boolean {
        if (r.access and Opcodes.ACC_INTERFACE != 0) {
            return false
        }
        if (r.access and Opcodes.ACC_ABSTRACT != 0) {
            return false
        }
        if (r.access and Opcodes.ACC_PUBLIC == 0) {
            return false
        }

        val ifaces = r.interfaces.orEmpty()
        if (ServerRule::class.jvmName.replace(".", "/") in ifaces) {
            return true
        }

        var interestingAnnotationFound = false
        r.accept(object : ClassVisitor(Opcodes.ASM5) {
            override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
                if (desc != null && desc.startsWith("L") && desc.removePrefix("L").removeSuffix(";").replace("/", ".") == BenchmarkServer::class.jvmName) {
                    interestingAnnotationFound = true
//                    return object : AnnotationVisitor(api) {
//                    }
                    return super.visitAnnotation(desc, visible)
                } else {
                    return super.visitAnnotation(desc, visible)
                }
            }
        }, 0)

        return interestingAnnotationFound
    }


    fun searchByKey(key: ServerRule.ServerApplicationKey): List<Class<ServerRule.ServerApplication>> =
            allInterestingClasses.filter { ServerRule.ServerApplication::class.java.isAssignableFrom(it) }
                    .map @Suppress("UNCHECKED_CAST") { it as Class<ServerRule.ServerApplication> }
                    .filter { key in it.newInstance().implementsApplications }

    private fun ClassLoader.allURLs(): List<URL> {
        val parentUrls = parent?.allURLs() ?: emptyList()
        if (this is URLClassLoader) {
            val urls = urLs.filterNotNull()
            return urls + parentUrls
        }
        return parentUrls
    }

    tailrec
    private fun findContainingZipFile(uri: URI): File {
        if (uri.scheme == "file") {
            return File(uri.path.substringBefore("!"))
        } else {
            return findContainingZipFile(URI(uri.rawSchemeSpecificPart))
        }
    }
}
