import Configuration from "../sections/Configuration"
import Installation from "../sections/Installation"
import Introduction from "../sections/Introduction"

function GettingStarted() {
    return (
        <div className="flex flex-col gap-5">
            <Introduction />
            <Installation />
            <Configuration />
        </div>
    )
}

export default GettingStarted