export type ConfirmationProps = {
    message: string;
    onConfirm: () => void;
    disabled: boolean;
    isPending?: boolean;
}
export function Confirm({
    message,
    onConfirm,
    disabled = false,
    isPending = false
}: ConfirmationProps) {
    return (
        <div className="confirmation">
            <p className="confirmation-prompt"> {message}</p>
            <button
                type="button"
                onClick={onConfirm}
                disabled={disabled || isPending}
                aria-busy={isPending}>
                
                Confirm</button>
            {isPending ? "Confirming..." : "Accept current price"}
        </div>
    )
}