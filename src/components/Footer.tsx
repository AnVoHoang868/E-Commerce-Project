const Footer = () => {
    return (
        <div>
            <div className='flex flex-col sm:grid grid-cols-[3fr_1fr_1fr] gap-14 my-10 mt-40 text-sm'>
                <div>
                    {/* Logo Text instead of Image for no-asset safety */}
                    <h1 className="text-xl font-bold mb-5 w-32 uppercase">Forever.</h1>
                    <p className='w-full md:w-2/3 text-gray-600'>
                        Forever is your destination for timeless fashion. We are committed to providing quality clothing that fits your lifestyle, ensuring you look good and feel great every day.
                    </p>
                </div>

                <div>
                    <p className='text-xl font-medium mb-5'>COMPANY</p>
                    <ul className='flex flex-col gap-1 text-gray-600'>
                        <li className="cursor-pointer hover:text-black">Home</li>
                        <li className="cursor-pointer hover:text-black">About us</li>
                        <li className="cursor-pointer hover:text-black">Delivery</li>
                        <li className="cursor-pointer hover:text-black">Privacy policy</li>
                    </ul>
                </div>

                <div>
                    <p className='text-xl font-medium mb-5'>GET IN TOUCH</p>
                    <ul className='flex flex-col gap-1 text-gray-600'>
                        <li>+1-212-456-7890</li>
                        <li>contact@forever.com</li>
                    </ul>
                </div>

            </div>

            <div>
                <hr />
                <p className='py-5 text-sm text-center'>Copyright 2024@ forever.com - All Right Reserved.</p>
            </div>

        </div>
    )
}

export default Footer
