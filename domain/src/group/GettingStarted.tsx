import Configuration from "../sections/gs/Configuration"
import Installation from "../sections/gs/Installation"
import Introduction from "../sections/gs/Introduction"

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