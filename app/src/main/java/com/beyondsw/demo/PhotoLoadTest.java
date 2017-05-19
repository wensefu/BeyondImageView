package com.beyondsw.demo;

/**
 * Created by wensefu on 2017/5/6.
 */

public class PhotoLoadTest {

    public static final String[][] rc_images ={

            {"http://i2.muimg.com/4862/6ffb3aa162fc3d80t.jpg","http://i2.muimg.com/4862/6ffb3aa162fc3d80.jpg"},
            {"http://i2.muimg.com/4862/1f147d4fd2296f92t.jpg","http://i2.muimg.com/4862/1f147d4fd2296f92.jpg"},
            {"http://i2.muimg.com/4862/fa058b0f35d8a2d6t.jpg","http://i2.muimg.com/4862/fa058b0f35d8a2d6.jpg"},
            {"http://i2.muimg.com/4862/51fc99e7984545cft.jpg","http://i2.muimg.com/4862/51fc99e7984545cf.jpg"},
            {"http://i2.muimg.com/4862/f5b5fdbf2d8d4f0bt.jpg","http://i2.muimg.com/4862/f5b5fdbf2d8d4f0b.jpg"},
            {"http://i2.muimg.com/4862/776f7507b2f13f65t.jpg","http://i2.muimg.com/4862/776f7507b2f13f65.jpg"},
            {"http://i2.muimg.com/4862/c67eb00732c1ef65t.jpg","http://i2.muimg.com/4862/c67eb00732c1ef65.jpg"},
            {"http://i2.muimg.com/4862/191b13a38efe3351t.jpg","http://i2.muimg.com/4862/191b13a38efe3351.jpg"},
            {"http://i2.muimg.com/4862/97582c7070cef744t.jpg","http://i2.muimg.com/4862/97582c7070cef744.jpg"},
            {"http://i2.muimg.com/4862/68fa6d23aa67d378t.jpg","http://i2.muimg.com/4862/68fa6d23aa67d378.jpg"},
            {"http://i2.muimg.com/4862/7f4460dc3be0d7aat.jpg","http://i2.muimg.com/4862/7f4460dc3be0d7aa.jpg"},
            {"http://i2.muimg.com/4862/0ee7938ef32b5faat.jpg","http://i2.muimg.com/4862/0ee7938ef32b5faa.jpg"},
            {"http://i2.muimg.com/4862/71a992470ec2c58dt.jpg","http://i2.muimg.com/4862/71a992470ec2c58d.jpg"},
            {"http://i2.muimg.com/4862/7851534db6a379e7t.jpg","http://i2.muimg.com/4862/7851534db6a379e7.jpg"},
            {"http://i2.muimg.com/4862/24e355d2bcdb4a43t.jpg","http://i2.muimg.com/4862/24e355d2bcdb4a43.jpg"},
            {"http://i2.muimg.com/4862/2ac26ce5573f3237t.jpg","http://i2.muimg.com/4862/2ac26ce5573f3237.jpg"},
            {"http://i2.muimg.com/4862/267335969bf5b77dt.jpg","http://i2.muimg.com/4862/267335969bf5b77d.jpg"},
            {"http://i2.muimg.com/4862/5be7dccac7e11823t.jpg","http://i2.muimg.com/4862/5be7dccac7e11823.jpg"},
            {"http://i2.muimg.com/4862/613f8473a4950585t.jpg","http://i2.muimg.com/4862/613f8473a4950585.jpg"},
            {"http://i2.muimg.com/4862/ce4eb224285cc959t.jpg","http://i2.muimg.com/4862/ce4eb224285cc959.jpg"},
            {"http://i2.muimg.com/4862/eed024556a757aadt.jpg","http://i2.muimg.com/4862/eed024556a757aad.jpg"},
            {"http://i2.muimg.com/4862/c6509ddd30d29989t.jpg","http://i2.muimg.com/4862/c6509ddd30d29989.jpg"},
            {"http://i2.muimg.com/4862/7d48ffaa628103f9t.jpg","http://i2.muimg.com/4862/7d48ffaa628103f9.jpg"},
            {"http://i2.muimg.com/4862/1c7641eff75d9e00t.jpg","http://i2.muimg.com/4862/1c7641eff75d9e00.jpg"},
            {"http://i2.muimg.com/4862/58e803d26e85069bt.jpg","http://i2.muimg.com/4862/58e803d26e85069b.jpg"},
            {"http://i2.muimg.com/4862/fe96d93aba9e31d1t.jpg","http://i2.muimg.com/4862/fe96d93aba9e31d1.jpg"},
            {"http://i2.muimg.com/4862/9a773412d7433986t.jpg","http://i2.muimg.com/4862/9a773412d7433986.jpg"},
            {"http://i2.muimg.com/4862/29c2ffd5c79f7e59t.jpg","http://i2.muimg.com/4862/29c2ffd5c79f7e59.jpg"},
            {"http://i2.muimg.com/4862/fa500a5757ff697ft.jpg","http://i2.muimg.com/4862/fa500a5757ff697f.jpg"},
            {"http://i2.muimg.com/4862/72af17fafed668dft.jpg","http://i2.muimg.com/4862/72af17fafed668df.jpg"},
            {"http://i2.muimg.com/4862/e0e488e94b86b787t.jpg","http://i2.muimg.com/4862/e0e488e94b86b787.jpg"},
            {"http://i2.muimg.com/4862/c95edca6700d6197t.jpg","http://i2.muimg.com/4862/c95edca6700d6197.jpg"},
            {"http://i2.muimg.com/4862/d5a35ad258fc2f4ct.jpg","http://i2.muimg.com/4862/d5a35ad258fc2f4c.jpg"},
            {"http://i2.muimg.com/4862/0182f165eac905eft.jpg","http://i2.muimg.com/4862/0182f165eac905ef.jpg"},
            {"http://i2.muimg.com/4862/b1f7b1d418d0f8bat.jpg","http://i2.muimg.com/4862/b1f7b1d418d0f8ba.jpg"},
            {"http://i2.muimg.com/4862/09129eb397d9de6ct.jpg","http://i2.muimg.com/4862/09129eb397d9de6c.jpg"},
            {"http://i2.muimg.com/4862/4ce98c6721319a7dt.jpg","http://i2.muimg.com/4862/4ce98c6721319a7d.jpg"},
            {"http://i2.muimg.com/4862/756525d1f4354992t.jpg","http://i2.muimg.com/4862/756525d1f4354992.jpg"},
            {"http://i2.muimg.com/4862/61cb617aa8c3501bt.jpg","http://i2.muimg.com/4862/61cb617aa8c3501b.jpg"},
            {"http://i2.muimg.com/4862/626d4af216f9d33et.jpg","http://i2.muimg.com/4862/626d4af216f9d33e.jpg"},
            {"http://i2.muimg.com/4862/64de982150cfad02t.jpg","http://i2.muimg.com/4862/64de982150cfad02.jpg"},
            {"http://i2.muimg.com/4862/2cc78d04187f65e7t.jpg","http://i2.muimg.com/4862/2cc78d04187f65e7.jpg"},
            {"http://i2.muimg.com/4862/5798081c23d6d05at.jpg","http://i2.muimg.com/4862/5798081c23d6d05a.jpg"},
            {"http://i2.muimg.com/4862/a718d50dc1b3c984t.jpg","http://i2.muimg.com/4862/a718d50dc1b3c984.jpg"},
    };



}
