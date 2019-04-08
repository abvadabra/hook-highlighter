import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JumpToTargetAction extends AnAction {

    public PsiMethod targetMethod;


    public JumpToTargetAction(@Nullable String text, @Nullable String description, @Nullable Icon icon,
                              PsiMethod targetMethod) {
        super(text, description, icon);
        this.targetMethod = targetMethod;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        NavigationUtil.openFileWithPsiElement(targetMethod, true, true);

    }
}
