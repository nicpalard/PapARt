package fr.inria.papart;

import fr.inria.papart.tools.Homography;
import codeanticode.glgraphics.GLGraphicsOffScreen;
import codeanticode.glgraphics.GLTexture;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PVector;
import toxi.geom.Matrix4x4;
import toxi.geom.Plane;
import toxi.geom.Ray3D;
import toxi.geom.ReadonlyVec3D;
import toxi.geom.Vec3D;

/**
 * This class implements a virtual screen. The position of the screen has to be
 * passed. It no longers handles a camera.
 *
 * @author jeremylaviole
 */
public class Screen {

    //       private PVector userPos = new PVector(-paperSheetWidth/2, -paperSheetHeight/2 +500, 300);
    //       private PVector userPos = new PVector(paperSheetWidth/2, paperSheetHeight/2, 500);
    //    public PVector userPos = new PVector(0, -700, 1300);
    private PApplet parent;
    // The current graphics
    public GLGraphicsOffScreen thisGraphics;
    // Position holding...
    private PVector initPos = null;
    private PMatrix3D initPosM = null;
    private float[] pos3D;
    private Vec3D posPaper;
    private PVector posPaperP;
    private PMatrix3D pos;
    private PVector size;
    private float scale;
    private Plane plane = new Plane();
    private static final int nbPaperPosRender = 4;
    private PVector[] paperPosRender1 = new PVector[nbPaperPosRender];
    protected Homography homography;
    protected Matrix4x4 transformationProjPaper;
    private float halfEyeDist = 20; // 2cm
    private boolean isDrawing = true;

    public Screen(PApplet parent, PVector size, float scale) {
        this(parent, size, scale, false, 1);
    }

    public Screen(PApplet parent, PVector size, float scale, boolean useAA, int AAValue) {
        thisGraphics = new GLGraphicsOffScreen(parent, (int) (size.x * scale), (int) (size.y * scale), useAA, AAValue);
        this.size = size.get();
        this.scale = scale;
        this.parent = parent;
        pos = new PMatrix3D();
        posPaper = new Vec3D();
        posPaperP = new PVector();
        initHomography();
//        initImageGetter();
    }

    public void setAutoUpdatePos(Camera camera, MarkerBoard board) {
        pos3D = camera.getPosPointer(board);
    }

    public void setManualUpdatePos() {
        pos3D = new float[16];
    }

    public boolean isDrawing() {
        return isDrawing;
    }

    public void setDrawing(boolean isDrawing) {
        this.isDrawing = isDrawing;
    }

    ////////////////// 3D SPACE TO PAPER HOMOGRAPHY ///////////////
    private void initHomography() {
        homography = new Homography(parent, 3, 3, 4);
        homography.setPoint(false, 0, new PVector(0, 0, 0));
        homography.setPoint(false, 1, new PVector(1, 0, 0));
        homography.setPoint(false, 2, new PVector(1, 1, 0));
        homography.setPoint(false, 3, new PVector(0, 1, 0));
    }

    public GLTexture getTexture() {
        return thisGraphics.getTexture();
    }

    public void initTouch(Projector proj) {
        computePlane(proj);
        computeHomography(proj);
    }

    public GLGraphicsOffScreen getGraphics() {
        return thisGraphics;
    }

    public GLGraphicsOffScreen initDraw(PVector userPos) {
        return initDraw(userPos, 40, 5000);
    }

    public GLGraphicsOffScreen initDraw(PVector userPos, float nearPlane, float farPlane) {
        return initDraw(userPos, nearPlane, farPlane, false, false, true);
    }

    // TODO: optionnal args.
    public GLGraphicsOffScreen initDraw(PVector userPos, float nearPlane, float farPlane, boolean isAnaglyph, boolean isLeft, boolean isOnly) {
        if (initPos == null) {
            initPos = posPaperP.get();
            initPosM = pos.get();
        }

        if (isOnly) {
            thisGraphics.beginDraw();
            thisGraphics.clear(0);
        }

//	float nearPlane = 10;
//	float farPlane = 2000 * scale;
        PVector paperCameraPos = new PVector();

        // get the position at the start of the program.
        PVector tmp = initPos.get();
        tmp.sub(posPaperP); //  tmp =  currentPos - initPos   (Position)

        // Get the current paperSheet position
        PMatrix3D newPos = pos.get();

        newPos.invert();
        newPos.m03 = 0;
        newPos.m13 = 0;
        newPos.m23 = 0;   // inverse of the Transformation (without position)

        PVector tmp2 = userPos.get();

        if (isAnaglyph) {
            tmp2.add(isLeft ? -halfEyeDist : halfEyeDist, 0, 0);
        }
        tmp2.mult(-scale);
        tmp2.add(tmp);

        newPos.mult(tmp2, paperCameraPos);

        // http://www.gamedev.net/topic/597564-view-and-projection-matrices-for-vr-window-using-head-tracking/
        thisGraphics.camera(paperCameraPos.x, paperCameraPos.y, paperCameraPos.z,
                paperCameraPos.x, paperCameraPos.y, 0,
                0, 1, 0);

        float nearFactor = nearPlane / paperCameraPos.z;

        float left = nearFactor * (-scale * size.x / 2f - paperCameraPos.x);
        float right = nearFactor * (scale * size.x / 2f - paperCameraPos.x);
        float top = nearFactor * (scale * size.y / 2f - paperCameraPos.y);
        float bottom = nearFactor * (-scale * size.y / 2f - paperCameraPos.y);

        thisGraphics.frustum(left, right, bottom, top, nearPlane, farPlane);

        return thisGraphics;
    }

    /**
     * TO remove ??
     *
     * @return
     */
    public GLGraphicsOffScreen initDrawLite() {
        if (initPos == null) {
            initPos = posPaperP.get();
            initPosM = pos.get();
        }

        thisGraphics.beginDraw();

        float nearPlane = 20;
        float farPlane = 2000;

        PVector paperCameraPos = new PVector();
        PVector userPos = new PVector();

        // get the position at the start of the program.
        PVector tmp = initPos.get();
        tmp.sub(posPaperP); //  tmp =  currentPos - initPos   (Position)

        // Get the current paperSheet position
        PMatrix3D newPos = pos.get();

        newPos.invert();
        newPos.m03 = 0;
        newPos.m13 = 0;
        newPos.m23 = 0;   // inverse of the Transformation (without position)

        PVector tmp2 = userPos.get();
        tmp2.mult(-scale);
        tmp2.add(tmp);

        newPos.mult(tmp2, paperCameraPos);

        // http://www.gamedev.net/topic/597564-view-and-projection-matrices-for-vr-window-using-head-tracking/
        thisGraphics.camera(paperCameraPos.x, paperCameraPos.y, paperCameraPos.z,
                paperCameraPos.x, paperCameraPos.y, 0,
                0, 1, 0);

        float nearFactor = nearPlane / paperCameraPos.z;

        float left = nearFactor * (-scale * size.x / 2f - paperCameraPos.x);
        float right = nearFactor * (scale * size.x / 2f - paperCameraPos.x);
        float top = nearFactor * (scale * size.y / 2f - paperCameraPos.y);
        float bottom = nearFactor * (-scale * size.y / 2f - paperCameraPos.y);

        thisGraphics.frustum(left, right, bottom, top, nearPlane, farPlane);

        return thisGraphics;
    }

    /**
     * Compute the position of the screen into the 3D scene.
     *
     * @param projector
     */
    protected void computeScreenPosition(Projector projector) {


        // TODO: optimisation no more new ?

        PMatrix3D mat = new PMatrix3D(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
//        proj.scale(1, 1, -1);  // or is it ?

        // go to the projector view || or stay camera view ?!
//        proj.apply(projector.projExtrinsicsP3D);

        // go to the paper place
        mat.apply(pos);
        paperPosRender1[0] = new PVector(mat.m03, mat.m13, mat.m23);
        mat.translate(size.x, 0, 0);
        paperPosRender1[1] = new PVector(mat.m03, mat.m13, mat.m23);
        mat.translate(0, size.y, 0);
        paperPosRender1[2] = new PVector(mat.m03, mat.m13, mat.m23);
        mat.translate(-size.x, 0, 0);
        paperPosRender1[3] = new PVector(mat.m03, mat.m13, mat.m23);


//        GLGraphicsOffScreen projGraphics = projector.getGraphics();
//        projGraphics.pushMatrix();
//        projGraphics.modelview.apply(projector.projExtrinsicsP3DInv); // camera view - instead of projector view
//        projGraphics.pushMatrix();
//        projGraphics.modelview.apply(pos);    // Go te the paper position
//
//        paperPosRender1[0] = posPaperP.get();
//
//        projGraphics.translate(size.x, 0, 0);
//        paperPosRender1[1] = new PVector(projGraphics.modelview.m03,
//                projGraphics.modelview.m13,
//                -projGraphics.modelview.m23);
//
//        projGraphics.translate(0, size.y, 0);
//        paperPosRender1[2] = new PVector(projGraphics.modelview.m03,
//                projGraphics.modelview.m13,
//                -projGraphics.modelview.m23);
//
//        projGraphics.translate(-size.x, 0, 0);
//        paperPosRender1[3] = new PVector(projGraphics.modelview.m03,
//                projGraphics.modelview.m13,
//                -projGraphics.modelview.m23);
//        projGraphics.popMatrix();
//
////        // ScreenX from camera view
////        for (int i = 0; i < nbPaperPosRender; i++) {
////            paperPosScreen[i] =
////                    new PVector(projGraphics.screenX(paperPosRender1[i].x, paperPosRender1[i].y, -paperPosRender1[i].z),
////                    projGraphics.screenY(paperPosRender1[i].x, paperPosRender1[i].y, -paperPosRender1[i].z),
////                    projGraphics.screenZ(paperPosRender1[i].x, paperPosRender1[i].y, -paperPosRender1[i].z));
////        }
//        projGraphics.popMatrix();
    }

    /////////////// NOTE : these 2 functions can be changed into a simple call... /////
    /**
     * Used for pointer projection
     *
     * @param pc
     */
    private void computeHomography(Projector pc) {
        computeScreenPosition(pc);
        for (int i = 0; i < 4; i++) {
            homography.setPoint(true, i, paperPosRender1[i]);
        }
        homography.compute();
        transformationProjPaper = homography.getTransformation();
    }

    /**
     * Get the position on the screen of a 3D point.
     *
     * @param v
     * @return
     */
    public Vec3D applyProjPaper(ReadonlyVec3D v) {
        return transformationProjPaper.applyTo(v);
    }

    ///////////////////// PLANE COMPUTATION  //////////////////
    private Plane computePlane(Projector projector) {

        PMatrix3D proj = new PMatrix3D(1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
        // create a modelview matrix, like the projector one.
//        proj.scale(1, 1, -1);  // or is it ?

        // go to the projector view || or stay camera view ?!
//        proj.apply(projector.projExtrinsicsP3D);

        // go to the paper place
        proj.apply(pos);

        PVector center = new PVector(proj.m03, proj.m13, proj.m23);

        // got a little higher for the normal.
        proj.translate(0, 0, 10);
        PVector normal = new PVector(proj.m03, proj.m13, proj.m23);

//        projector.projExtrinsicsP3DInv.mult(center, center);
//        projector.projExtrinsicsP3DInv.mult(normal, normal);
//
//             System.out.println("normale 1" + normal);

        plane.set(new Vec3D(center.x, center.y, center.z));
        plane.normal.set(new Vec3D(normal.x, normal.y, normal.z));

//        System.out.println("Plane center " + center);
//        System.out.println("Plane normal " + normal);
//        System.out.println("Pos paper : " + posPaper);

//   
//        GLGraphicsOffScreen projGraphics = projector.getGraphics();
//        projGraphics.pushMatrix();
//        projGraphics.resetMatrix();
//
//        projGraphics.modelview.apply(pos);    // Go te the paper position
//        projGraphics.translate(0, 0, 10);
//
//        // Do the TWO INVERT operations,  invert Z again and apply the inverse of the projExtrinsics
//        PMatrix3D mv = projGraphics.modelview;
//        
//        PVector p1 = new PVector(mv.m03, mv.m13, mv.m23);  // get the current Point
//        PVector normale = new PVector();
//        projector.projExtrinsicsP3DInv.mult(p1, normale);   // move the currentPoint 
//
//        System.out.println("normale 2" + normale);
//
////        plane.set(posPaper);
////        plane.normal.set(new Vec3D(normale.x, normale.y, normale.z));
//        //    screenGFX.plane(plane, 100);
//        projGraphics.popMatrix();

//        System.out.println("Plane : " + plane);

        return plane;
    }

    ///////////////////// POINTER PROJECTION  ////////////////
    // GluUnproject
    // TODO: not working ???
    public ReadonlyVec3D projectMouse(Projector projector, int mouseX, int mouseY, int width, int height) {

        GLGraphicsOffScreen projGraphics = projector.getGraphics();
        PMatrix3D projMat = projector.projectionInit.get();
        PMatrix3D modvw = projGraphics.modelview.get();

        double[] mouseDist = projector.proj.undistort(mouseX, mouseY);
        float x = 2 * (float) mouseDist[0] / (float) width - 1;
        float y = 2 * (float) mouseDist[1] / (float) height - 1;

        PVector vect = new PVector(x, y, 1);
        PVector transformVect = new PVector();
        PVector transformVect2 = new PVector();
        projMat.apply(modvw);
        projMat.invert();
        projMat.mult(vect, transformVect);
        vect.z = (float) 0.85;
        projMat.mult(vect, transformVect2);
        //    println(skip / 10f);
        Ray3D ray = new Ray3D(new Vec3D(transformVect.x, transformVect.y, transformVect.z),
                new Vec3D(transformVect2.x, transformVect2.y, transformVect2.z));

        ReadonlyVec3D res = plane.getIntersectionWithRay(ray);
        return res;
    }

    // TODO: more doc...
    /**
     * Projects the position of a pointer in normalized screen space.
     * If you need to undistort the pointer, do so before passing px and py.
     *
     * @param px Normalized x position (0,1) in projector space
     * @param py Normalized y position (0,1) in projector space
     * @return Position of the pointer.
     */
    public PVector projectPointer(Projector projector, float px, float py) {

        float x = px * 2 - 1;
        float y = py * 2 - 1;

        // First get the projector transformation. 
        PMatrix3D projIntr = projector.getProjectionInit().get();
        projIntr.scale(1, 1, -1);
        // Set to the origin, as the plane was computed from the origin
        projIntr.apply(projector.getExtrinsics());
        
        // invert for the inverse projection
        projIntr.invert();

        // We get a 3D ray from the position 
        PVector p1 = new PVector(x, y, -1);
        PVector p2 = new PVector(x, y, 1f);
        PVector out1 = new PVector();
        PVector out2 = new PVector();
        // z also between -1 and 1f

        // view of the point from the projector.
        Utils.mult(projIntr, p1, out1);
        Utils.mult(projIntr, p2, out2);

        Ray3D ray = new Ray3D(new Vec3D(out1.x, out1.y, out1.z),
                new Vec3D(out2.x, out2.y, out2.z));

        ReadonlyVec3D res = plane.getIntersectionWithRay(ray);
        if (res == null) {
            return null;
        }

        res = transformationProjPaper.applyTo(res);
        PVector out = new PVector(res.x() / res.z(),
                res.y() / res.z(), 0);
        return out;
    }

    public float getHalfEyeDist() {
        return halfEyeDist;
    }

    public void setHalfEyeDist(float halfEyeDist) {
        this.halfEyeDist = halfEyeDist;
    }

    public PVector getSize() {
        return size;
    }

    public PMatrix3D getPos() {
        return pos;
    }

    public float getScale() {
        return this.scale;
    }

    // Available only if pos3D is being updated elsewhere...
    public void updatePos() {


        pos = new PMatrix3D(pos3D[0], pos3D[1], pos3D[2], pos3D[3],
                pos3D[4], pos3D[5], pos3D[6], pos3D[7],
                pos3D[8], pos3D[9], pos3D[10], pos3D[11],
                0, 0, 0, 1);
//        pos.m00 = pos3D[0];
//        pos.m01 = pos3D[1];
//        pos.m02 = pos3D[2];
//        pos.m03 = pos3D[3];
//        pos.m10 = pos3D[4];
//        pos.m11 = pos3D[5];
//        pos.m12 = pos3D[6];
//        pos.m13 = pos3D[7];
//        pos.m20 = pos3D[8];
//        pos.m12 = pos3D[9];
//        pos.m22 = pos3D[10];
//        pos.m23 = pos3D[11];

        posPaper.x = pos3D[3];
        posPaper.y = pos3D[7];
        posPaper.z = pos3D[11];

        posPaperP.x = pos3D[3];
        posPaperP.y = pos3D[7];
        posPaperP.z = pos3D[11];
    }

    public void updatePosT() {

        pos.m00 = pos3D[0];
        pos.m01 = pos3D[4];
        pos.m02 = pos3D[8];

        pos.m10 = pos3D[1];
        pos.m11 = pos3D[5];
        pos.m12 = pos3D[9];

        pos.m20 = pos3D[2];
        pos.m12 = pos3D[6];
        pos.m22 = pos3D[10];

        pos.m03 = pos3D[3];
        pos.m13 = pos3D[7];
        pos.m23 = pos3D[11];

        posPaper.x = pos3D[3];
        posPaper.y = pos3D[7];
        posPaper.z = pos3D[11];

        posPaperP.x = pos3D[3];
        posPaperP.y = pos3D[7];
        posPaperP.z = pos3D[11];
    }

    public void setPos(float pos3D[]) {

        // TODO: not optimal, need to check the pos3D creation / deletion
        this.pos3D = pos3D;
        pos.m00 = pos3D[0];
        pos.m01 = pos3D[1];
        pos.m02 = pos3D[2];
        pos.m03 = pos3D[3];
        pos.m10 = pos3D[4];
        pos.m11 = pos3D[5];
        pos.m12 = pos3D[6];
        pos.m13 = pos3D[7];
        pos.m20 = pos3D[8];
        pos.m12 = pos3D[9];
        pos.m22 = pos3D[10];
        pos.m23 = pos3D[11];

        posPaper.x = pos3D[3];
        posPaper.y = pos3D[7];
        posPaper.z = pos3D[11];

        posPaperP.x = pos3D[3];
        posPaperP.y = pos3D[7];
        posPaperP.z = pos3D[11];

    }
}