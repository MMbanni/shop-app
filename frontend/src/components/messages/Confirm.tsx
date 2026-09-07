export type ConfirmationProps = {
    message: string;
    onConfirm: ()=> void 
}
export function Confirm(props: ConfirmationProps){
    return (
        <div className="confirmation">
            <p className="confirmation-prompt"> {props.message}</p>
            <button onClick={props.onConfirm}>Confirm</button>
        </div>
    )
}