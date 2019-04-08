import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.impl.util.GutterActionRenderer;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HookOutAnnotator implements Annotator {

    public static final Icon gutterIcon = IconLoader.getIcon("icons/icon_out.png");
    private static final Logger log = Logger.getInstance("HookPlugin");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if(!(element instanceof PsiMethod))
            return;

        PsiMethod method = (PsiMethod) element;

        PsiModifierList modifierList = method.getModifierList();
        PsiAnnotation annotation = modifierList.findAnnotation("gloomyfolken.hooklib.asm.Hook");
        if(annotation == null)
            return;

        JvmParameter[] parameters = method.getParameters();
        if(parameters.length == 0)
            return;

        JvmParameter target = parameters[0];
        JvmType type = target.getType();
        if(!(type instanceof PsiType))
            return;

        String targetClass = ((PsiType) type).getCanonicalText();
        String methodName = method.getName();

        List<JvmAnnotationAttribute> attributes = annotation.getAttributes();
        for (JvmAnnotationAttribute attribute : attributes) {
            if(!attribute.getAttributeName().equals("targetMethod"))
                continue;

            JvmAnnotationAttributeValue value = attribute.getAttributeValue();
            if(value instanceof JvmAnnotationConstantValue){
                Object constantValue = ((JvmAnnotationConstantValue) value).getConstantValue();
                if(constantValue instanceof String)
                    methodName = (String) constantValue;
            }
            break;

        }

        String targetInfo = targetClass + "." + methodName;

        Annotation infoAnnotation = holder.createInfoAnnotation(element, null);
        PsiMethod targetMethod = findPsiMethodByReference(element.getProject(), targetClass, methodName);
        if(targetMethod != null) {
            infoAnnotation.setGutterIconRenderer(new GutterActionRenderer(new JumpToTargetAction(
                    "Go to target: " + targetInfo, "", gutterIcon,
                    targetMethod
            )));
        }

//        infoAnnotation.setEnforcedTextAttributes(new TextAttributes(null, null,
//                JBColor.RED, EffectType.BOXED, Font.PLAIN));
    }

    private PsiMethod findPsiMethodByReference(Project project, String targetClass, String targetMethod){
        if(project == null){
            log.warn("Project is null");
            return null;
        }
        PsiClass psiClass = ClassUtil.findPsiClass(PsiManager.getInstance(project), targetClass);
        if(psiClass == null){
            log.warn("Cannot find class " + targetClass);
            return null;
        }


        PsiMethod[] methodsByName = psiClass.findMethodsByName(targetMethod, false);
        if(methodsByName.length == 0){
            log.warn("Unable to find method " + targetMethod + " in class " + targetClass);
            return null;
        }

        return methodsByName[0];
    }

}