import { Loader2 } from 'lucide-react';

function UnderConstruction() {
    return (
        <div className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Loader2 className="w-8 h-8 text-blue-400 mr-3 animate-spin" />
                <h1 className="text-3xl font-bold text-blue-400">Under Construction</h1>
            </div>

            <p className="text-gray-300 mb-1">
                This section is still being built. Check back soon for more content!
            </p>
        </div>
    );
}

export default UnderConstruction;