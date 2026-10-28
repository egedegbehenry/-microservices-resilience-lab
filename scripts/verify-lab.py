"""HTTP acceptance suite. Runs only against explicitly selected local lab instances."""
import argparse, concurrent.futures, json, time, urllib.request, urllib.error
from pathlib import Path

p=argparse.ArgumentParser()
p.add_argument('--upstream',default='http://127.0.0.1:8081')
p.add_argument('--downstream',default='http://127.0.0.1:8082')
p.add_argument('--output',default='test-results/acceptance.json')
args=p.parse_args(); A=args.upstream; B=args.downstream
for url in [A,B]:
    if not url.startswith('http://127.0.0.1:') and not url.startswith('http://localhost:'):
        raise ValueError('Local lab URLs required')
results=[]
def request(base,path,body=None):
    req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=30) as r: return r.status,r.read().decode()
    except urllib.error.HTTPError as e: return e.code,e.read().decode()
def state(base):
    status,body=request(base,'/lab/state'); assert status==200,body
    return json.loads(body)
def config(base,mode='success',failures=0,delay=0):
    status,body=request(base,'/lab/config',{'mode':mode,'failures':failures,'delayMs':delay}); assert status==200,body
def reset():
    for base in [A,B]:
        assert request(base,'/lab/reset',{})[0]==200
    # Reset clears breakers, not rate limiter permits.
    time.sleep(1.1)
def call(base,expected):
    status,body=request(base,'/lab/operation'); assert status==expected,(status,body,expected)
def pair(a,b,label):
    sa,sb=state(A),state(B)
    assert (sa['state'],sb['state'])==(a,b),(label,sa,sb)
    results.append({'scenario':label,'A':sa,'B':sb})
    print(label,a,b,flush=True)
def open_breaker(base):
    config(base,'failure')
    for _ in range(4): call(base,503)
    assert state(base)['state']=='OPEN'
    before=state(base)['attempts']; call(base,503)
    assert state(base)['attempts']==before
def wait_half(*bases):
    deadline=time.monotonic()+15
    while time.monotonic()<deadline:
        if all(state(base)['state']=='HALF_OPEN' for base in bases): return
        time.sleep(.1)
    raise AssertionError('HALF_OPEN deadline exceeded')

try:
    for base,path in [(A,'/api/bookings/availability'),(B,'/api/availability/status'),(B,'/api/availability/check')]:
        assert request(base,path)[0]==200,(base,path)
    for base,path in [(A,'/api/bookings/availability/failure'),(B,'/api/availability/simulate/failure')]:
        assert request(base,path)[0]==503,(base,path)
    started=time.monotonic()
    assert request(A,'/api/bookings/availability/delay')==(200,'AVAILABLE')
    results.append({'scenario':'preserved five-second delay through upstream with default eight-second read timeout',
                    'elapsedSeconds':round(time.monotonic()-started,3)})
    for base in [A,B]:
        for endpoint in ['/actuator/circuitbreakers','/actuator/circuitbreakerevents','/actuator/metrics']:
            assert request(base,endpoint)[0]==200,(base,endpoint)
    reset(); call(A,200); pair('CLOSED','CLOSED','1 normal HTTP communication')
    reset(); open_breaker(B); pair('CLOSED','OPEN','2 downstream independently open')
    reset(); open_breaker(A); pair('OPEN','CLOSED','3 upstream independently open')
    open_breaker(B); pair('OPEN','OPEN','4 both open')
    reset(); open_breaker(A); config(A); wait_half(A); pair('HALF_OPEN','CLOSED','5 upstream probes')
    call(A,200); call(A,200); pair('CLOSED','CLOSED','upstream successful recovery')
    reset(); open_breaker(B); config(B); wait_half(B); pair('CLOSED','HALF_OPEN','6 downstream probes')
    call(B,200); call(B,200); pair('CLOSED','CLOSED','downstream successful recovery')
    reset(); open_breaker(A); open_breaker(B); config(A); config(B); wait_half(A,B)
    pair('HALF_OPEN','HALF_OPEN','7 simultaneous half open')
    call(A,200); call(A,200); pair('CLOSED','CLOSED','both recovered through HTTP')
    for base in [A,B]:
        reset(); open_breaker(base); wait_half(base)
        call(base,503); call(base,503); assert state(base)['state']=='OPEN'
        results.append({'scenario':'failed half open recovery','service':base,'state':state(base)})
    for base in [A,B]:
        reset(); config(base,'transient',2); call(base,200)
        assert state(base)['attempts']==3
        results.append({'scenario':'retry succeeds on third attempt','service':base,'state':state(base)})
        reset(); config(base,'failure'); call(base,503); assert state(base)['attempts']==3
        results.append({'scenario':'retry exhausted at three attempts','service':base,'state':state(base)})
    reset(); config(B,'delay',delay=100)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        statuses=list(pool.map(lambda _:request(A,'/lab/operation')[0],range(8)))
    assert statuses==[200]*8,statuses
    results.append({'scenario':'eight simultaneous upstream requests','statuses':statuses})
    for base in [A,B]:
        reset()
        with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
            statuses=list(pool.map(lambda _:request(base,'/lab/operation')[0],range(60)))
        assert 429 in statuses,statuses
        results.append({'scenario':'bounded rate limit burst','service':base,'statuses':{str(s):statuses.count(s) for s in set(statuses)},'state':state(base)})
    reset()
finally:
    out=Path(args.output); out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps({'timestamp':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'results':results},indent=2))
