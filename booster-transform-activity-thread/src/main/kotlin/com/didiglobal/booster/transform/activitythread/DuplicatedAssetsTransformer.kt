package com.didiglobal.booster.transform.activitythread

import com.didiglobal.booster.kotlinx.asIterable
import com.didiglobal.booster.transform.TransformContext
import com.didiglobal.booster.transform.asm.ClassTransformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

class DuplicatedAssetsTransformer : ClassTransformer {

    private lateinit var mapping: Map<String, String>

    override fun transform(context: TransformContext, klass: ClassNode): ClassNode {
        if (klass.name == SHADOW_ASSET_MANAGER) {
            klass.methods.find {
                "${it.name}${it.desc}" == "<clinit>()V"
            }?.let { clinit ->
                klass.methods.remove(clinit)
            }

            klass.defaultClinit.let { clinit ->
                clinit.instructions.apply {
                    add(TypeInsnNode(Opcodes.NEW, "java/util/HashMap"))
                    add(InsnNode(Opcodes.DUP))
                    add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false))
                    add(VarInsnNode(Opcodes.ASTORE, 0))
                    mapping.forEach { shadow, real ->
                        add(VarInsnNode(Opcodes.ALOAD, 0))
                        add(LdcInsnNode(shadow))
                        add(LdcInsnNode(real))
                        add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false))
                        add(InsnNode(Opcodes.POP))
                    }
                    add(VarInsnNode(Opcodes.ALOAD, 0))
                    add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;", false))
                    add(FieldInsnNode(Opcodes.PUTSTATIC, SHADOW_ASSET_MANAGER, "DUPLICATED_ASSETS", "Ljava/util/Map;"))
                    add(InsnNode(Opcodes.RETURN))
                }
            }

        } else {
            klass.methods.forEach { method ->
                method.instructions?.iterator()?.asSequence()?.filterIsInstance(MethodInsnNode::class.java)?.filter {
                    ASSET_MANAGER == it.owner && "open(Ljava/lang/String;)Ljava/io/InputStream;" == "${it.name}${it.desc}"
                }?.forEach {
                    it.owner = SHADOW_ASSET_MANAGER
                    it.desc = "(L$ASSET_MANAGER;Ljava/lang/String;)Ljava/io/InputStream;"
                    it.opcode = Opcodes.INVOKESTATIC
                }
            }
        }

        return klass
    }

}

const val SHADOW_ASSET_MANAGER = ""
const val ASSET_MANAGER = ""
