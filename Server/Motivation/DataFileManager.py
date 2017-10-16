import os

def fetch_latest_file(rootdir, extname='.dat'):
    filelist = os.listdir(rootdir)
    filelist.sort(key=lambda x: os.path.getmtime(rootdir+'/'+x) if not os.path.isdir(rootdir+'/'+x) else 0)
    print(filelist)
    for i in range(len(filelist)-1, -1, -1):
        if filelist[i].find(extname) != -1:
            # print("Find file: " + filelist[i])
            return filelist[i]