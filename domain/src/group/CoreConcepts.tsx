import IoC from "../sections/cc/IoC"
import JoltApplication from "../sections/cc/JoltApplication"
import JoltContext from "../sections/cc/JoltContext"
import Routes from "../sections/cc/Routes"

function CoreConcepts() {
    return (
        <div className="flex flex-col gap-5">
            <JoltApplication />
            <Routes />
            <JoltContext />
            <IoC />
        </div>
    )
}

export default CoreConcepts