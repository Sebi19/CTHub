import {Button, type ButtonProps} from "@mantine/core";
import {IconArrowLeft} from "@tabler/icons-react";
import {useTranslation} from "react-i18next";

interface NavigateBackButtonProps extends Omit<ButtonProps, 'children'> {
    handleBackNavigation: () => void;
}

export const NavigateBackButton = ({handleBackNavigation, ...buttonProps}: NavigateBackButtonProps) => {
    const {t} = useTranslation();

    return (
        <Button
            variant="subtle"
            color="gray"
            leftSection={<IconArrowLeft size={16} />}
            onClick={handleBackNavigation}
            mb="md"
            px={0}
            {...buttonProps}
        >
            {t('app.common.navigation.back')}
        </Button>
    );
}