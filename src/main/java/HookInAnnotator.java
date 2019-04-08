import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diff.impl.util.GutterActionRenderer;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HookInAnnotator implements Annotator {

    public static final Icon gutterIcon = IconLoader.getIcon("icons/icon_in.png");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if(!(element instanceof PsiMethod))
            return;

        PsiMethod method = (PsiMethod) element;

        String parentClassName = ((PsiClass) method.getParent()).getQualifiedName();
        if(parentClassName == null || parentClassName.startsWith("net."))
            return;

        String thisMethodName =  method.getName();

        Project project = method.getProject();

        Module module = ModuleUtil.findModuleForFile(element.getContainingFile());
        if(module == null)
            return;

        GlobalSearchScope scope = GlobalSearchScope.moduleScope(module);
        PsiClass annotationClass = JavaPsiFacade.getInstance(project)
                .findClass("gloomyfolken.hooklib.asm.Hook", scope);
        if(annotationClass == null)
            return;

        List<PsiMethod> hookMethods = new ArrayList<>();

        AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope).forEach(annotatedMethod -> {
            PsiAnnotation ann = annotatedMethod.getModifierList().findAnnotation("gloomyfolken.hooklib.asm.Hook");

            JvmParameter[] parameters = annotatedMethod.getParameters();
            if(parameters.length == 0)
                return;

            JvmParameter target = parameters[0];
            JvmType type = target.getType();
            if(!(type instanceof PsiType))
                return;

            String targetClass = ((PsiType) type).getCanonicalText();
            if(!targetClass.equals(parentClassName))
                return;

            String methodName = annotatedMethod.getName();

            List<JvmAnnotationAttribute> attributes = ann.getAttributes();
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

            if(methodName.equals(thisMethodName))
                hookMethods.add(annotatedMethod);
        });

        if(hookMethods.isEmpty())
            return;


        Annotation infoAnnotation = holder.createInfoAnnotation(element, null);
        if(hookMethods.size() > 1) {
            infoAnnotation.setGutterIconRenderer(new GutterHookListIcon(new DefaultActionGroup(hookMethods.stream()
                    .map(it -> {
                        String text = ((PsiClass) it.getParent()).getQualifiedName() + "." + it.getName();
                        return new JumpToTargetAction(text, null, it.getIcon(0), it);
                    }).collect(Collectors.toList()))));
        } else {
            PsiMethod it = hookMethods.get(0);
            String text = ((PsiClass) it.getParent()).getQualifiedName() + "." + it.getName();
            infoAnnotation.setGutterIconRenderer(new GutterActionRenderer(
                    new JumpToTargetAction(text, null, gutterIcon, it)
            ));
        }
//        infoAnnotation.setEnforcedTextAttributes(new TextAttributes(null, null,
//                JBColor.GREEN, EffectType.BOXED, Font.PLAIN));
    }

    private class GutterHookListIcon extends GutterIconRenderer{

        public DefaultActionGroup group;

        public GutterHookListIcon(DefaultActionGroup group) {
            this.group = group;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GutterHookListIcon that = (GutterHookListIcon) o;
            return Objects.equals(group, that.group);
        }

        @Nullable
        @Override
        public ActionGroup getPopupMenuActions() {
            return group;
        }

        @Override
        public int hashCode() {
            return Objects.hash(group);
        }

        @NotNull
        @Override
        public Icon getIcon() {
            return gutterIcon;
        }
    }

}
