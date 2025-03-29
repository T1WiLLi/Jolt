import { BookOpen, Code, Filter, Layers, Link, Navigation, Rocket, Shield } from 'lucide-react';

function Introduction() {
    return (
        <div id="introduction" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <BookOpen className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">Introduction</h1>
            </div>

            <p className="text-gray-300 mb-6">
                Welcome to Jolt, a lightweight and powerful Java web framework designed to simplify the development of modern web applications. Jolt provides a robust set of tools and features to help you build scalable, maintainable, and high-performance applications with ease.
            </p>

            <p className="text-gray-300 mb-4">
                <span className="text-xl font-bold text-blue-400">A bit about Jolt</span><br />
                Hi ! I'm T1WiLLi, the creator of Jolt. I started working on Jolt, not because I wanted to create somethings for others, but because at the time I was actually working
                a lot with the Spring framework, which, I must say, is probably the best Java framework out there. However, people that know me, know that I've always loved to learn and
                experiment with new things. So, as I started learning Spring, I also started to wonder, how did they do this ? How did they make it so powerful ? And, I started to think,
                what if I could do something similar, but with a more lightweight approach ? And, that's how Jolt was born. Of course, the idea of creating a new framework was a bit scary
                at first, but, I was convinced that I could make something that would help me get a better understanding of how web frameworks work, and, maybe, even help others.
            </p>
            <span className="text-blue-400 font-bold mb-4">And, that's exactly what I did.</span>

            <hr className="text-blue-400 my-4" />

            <h2 className="text-xl font-semibold text-gray-200 mb-1">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Simple and intuitive API for rapid development</span>
                </li>
                <li className="flex items-start">
                    <Layers className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Built-in dependency injection for better modularity</span>
                </li>
                <li className="flex items-start">
                    <Navigation className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Powerful routing system for handling complex URL patterns</span>
                </li>
                <li className="flex items-start">
                    <Filter className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Support for filters for request processing</span>
                </li>
                <li className="flex items-start">
                    <Link className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">Seamless integration with external APIs and services</span>
                </li>
                <li className="flex items-start">
                    <Shield className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">High concern for security and data protection</span>
                </li>
            </ul>

            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Get Started</h3>
                    <p className="text-gray-400">
                        Ready to dive in? Check out the{' '}
                        <a href="#installation" className="text-blue-400 hover:underline">
                            Installation
                        </a>{' '}
                        section to set up Jolt in your project and start building amazing applications today.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Introduction;